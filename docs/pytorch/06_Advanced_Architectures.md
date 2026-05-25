# 6. Advanced Architectures

## StyleGAN-style Generator

```python
class MappingNetwork(nn.Module):
    """Maps latent z to intermediate w space."""
    def __init__(self, z_dim=512, w_dim=512, num_layers=8):
        super().__init__()
        layers = []
        for i in range(num_layers):
            layers.append(nn.Linear(z_dim if i == 0 else w_dim, w_dim))
            layers.append(nn.LeakyReLU(0.2))
        self.net = nn.Sequential(*layers)
    
    def forward(self, z):
        return self.net(z)


class AdaIN(nn.Module):
    """Adaptive Instance Normalization — style injection."""
    def __init__(self, channels, w_dim):
        super().__init__()
        self.norm = nn.InstanceNorm2d(channels)
        self.style = nn.Linear(w_dim, channels * 2)  # scale + shift
    
    def forward(self, x, w):
        style = self.style(w).unsqueeze(-1).unsqueeze(-1)
        gamma, beta = style.chunk(2, dim=1)
        return gamma * self.norm(x) + beta


class StyleBlock(nn.Module):
    def __init__(self, in_channels, out_channels, w_dim, upsample=True):
        super().__init__()
        self.upsample = nn.Upsample(scale_factor=2) if upsample else nn.Identity()
        self.conv = nn.Conv2d(in_channels, out_channels, 3, padding=1)
        self.adain = AdaIN(out_channels, w_dim)
        self.activation = nn.LeakyReLU(0.2)
        self.noise_scale = nn.Parameter(torch.zeros(1, out_channels, 1, 1))
    
    def forward(self, x, w):
        x = self.upsample(x)
        x = self.conv(x)
        # Inject noise
        noise = torch.randn_like(x) * self.noise_scale
        x = x + noise
        x = self.adain(x, w)
        return self.activation(x)
```

---

## Denoising Diffusion (DDPM)

```python
class DiffusionSchedule:
    """Linear noise schedule for DDPM."""
    def __init__(self, timesteps=1000, beta_start=1e-4, beta_end=0.02, device='cuda'):
        self.timesteps = timesteps
        betas = torch.linspace(beta_start, beta_end, timesteps, device=device)
        alphas = 1.0 - betas
        alphas_cumprod = torch.cumprod(alphas, dim=0)
        
        self.betas = betas
        self.alphas = alphas
        self.alphas_cumprod = alphas_cumprod
        self.sqrt_alphas_cumprod = torch.sqrt(alphas_cumprod)
        self.sqrt_one_minus_alphas_cumprod = torch.sqrt(1.0 - alphas_cumprod)
    
    def add_noise(self, x0, t, noise=None):
        """Forward diffusion: q(x_t | x_0)."""
        if noise is None:
            noise = torch.randn_like(x0)
        sqrt_alpha = self.sqrt_alphas_cumprod[t][:, None, None, None]
        sqrt_one_minus = self.sqrt_one_minus_alphas_cumprod[t][:, None, None, None]
        return sqrt_alpha * x0 + sqrt_one_minus * noise, noise


class UNetDiffusion(nn.Module):
    """U-Net noise predictor with time conditioning."""
    
    def __init__(self, in_channels=3, base_channels=64, time_dim=256):
        super().__init__()
        # Time embedding
        self.time_mlp = nn.Sequential(
            SinusoidalPositionEmbedding(time_dim),
            nn.Linear(time_dim, time_dim),
            nn.GELU(),
            nn.Linear(time_dim, time_dim),
        )
        
        # Encoder
        self.enc1 = ResBlock(in_channels, base_channels, time_dim)
        self.enc2 = ResBlock(base_channels, base_channels * 2, time_dim)
        self.enc3 = ResBlock(base_channels * 2, base_channels * 4, time_dim)
        self.down1 = nn.Conv2d(base_channels, base_channels, 3, stride=2, padding=1)
        self.down2 = nn.Conv2d(base_channels * 2, base_channels * 2, 3, stride=2, padding=1)
        
        # Middle
        self.mid = ResBlock(base_channels * 4, base_channels * 4, time_dim)
        self.mid_attn = SelfAttention(base_channels * 4)
        
        # Decoder
        self.up2 = nn.ConvTranspose2d(base_channels * 4, base_channels * 2, 2, stride=2)
        self.dec2 = ResBlock(base_channels * 4, base_channels * 2, time_dim)
        self.up1 = nn.ConvTranspose2d(base_channels * 2, base_channels, 2, stride=2)
        self.dec1 = ResBlock(base_channels * 2, base_channels, time_dim)
        
        self.final = nn.Conv2d(base_channels, in_channels, 1)
    
    def forward(self, x, t):
        t_emb = self.time_mlp(t)
        
        # Encoder
        e1 = self.enc1(x, t_emb)
        e2 = self.enc2(self.down1(e1), t_emb)
        e3 = self.enc3(self.down2(e2), t_emb)
        
        # Middle
        m = self.mid(e3, t_emb)
        m = self.mid_attn(m)
        
        # Decoder
        d2 = self.dec2(torch.cat([self.up2(m), e2], dim=1), t_emb)
        d1 = self.dec1(torch.cat([self.up1(d2), e1], dim=1), t_emb)
        
        return self.final(d1)


class SinusoidalPositionEmbedding(nn.Module):
    def __init__(self, dim):
        super().__init__()
        self.dim = dim
    
    def forward(self, t):
        half_dim = self.dim // 2
        emb = math.log(10000) / (half_dim - 1)
        emb = torch.exp(torch.arange(half_dim, device=t.device) * -emb)
        emb = t[:, None].float() * emb[None, :]
        return torch.cat([emb.sin(), emb.cos()], dim=-1)


class ResBlock(nn.Module):
    def __init__(self, in_c, out_c, time_dim):
        super().__init__()
        self.conv1 = nn.Sequential(nn.GroupNorm(8, in_c), nn.SiLU(), nn.Conv2d(in_c, out_c, 3, padding=1))
        self.time_proj = nn.Linear(time_dim, out_c)
        self.conv2 = nn.Sequential(nn.GroupNorm(8, out_c), nn.SiLU(), nn.Conv2d(out_c, out_c, 3, padding=1))
        self.skip = nn.Conv2d(in_c, out_c, 1) if in_c != out_c else nn.Identity()
    
    def forward(self, x, t_emb):
        h = self.conv1(x)
        h = h + self.time_proj(t_emb)[:, :, None, None]
        h = self.conv2(h)
        return h + self.skip(x)


# Training
schedule = DiffusionSchedule(timesteps=1000)
model = UNetDiffusion().cuda()
optimizer = torch.optim.AdamW(model.parameters(), lr=1e-4)

for images in dataloader:
    images = images.cuda()
    t = torch.randint(0, 1000, (images.shape[0],), device='cuda')
    noisy, noise = schedule.add_noise(images, t)
    
    predicted_noise = model(noisy, t)
    loss = F.mse_loss(predicted_noise, noise)
    
    loss.backward()
    optimizer.step()
    optimizer.zero_grad()
```

---

## State-Space Model (Mamba-style)

```python
class SelectiveSSM(nn.Module):
    """Simplified Mamba-style selective state-space model.
    Linear-time sequence modeling (alternative to Transformers).
    """
    
    def __init__(self, d_model, d_state=16, d_conv=4, expand=2):
        super().__init__()
        d_inner = int(expand * d_model)
        
        self.in_proj = nn.Linear(d_model, d_inner * 2, bias=False)
        
        # Convolution for local context
        self.conv1d = nn.Conv1d(d_inner, d_inner, d_conv, padding=d_conv-1, groups=d_inner)
        
        # SSM parameters (input-dependent — "selective")
        self.x_proj = nn.Linear(d_inner, d_state * 2 + 1, bias=False)  # B, C, dt
        self.dt_proj = nn.Linear(1, d_inner, bias=True)
        
        # State matrix A (structured — diagonal)
        A = torch.arange(1, d_state + 1).float()
        self.A_log = nn.Parameter(torch.log(A))
        self.D = nn.Parameter(torch.ones(d_inner))
        
        self.out_proj = nn.Linear(d_inner, d_model, bias=False)
    
    def forward(self, x):
        """x: (B, L, D)"""
        B, L, D = x.shape
        
        # Input projection
        xz = self.in_proj(x)
        x, z = xz.chunk(2, dim=-1)
        
        # Conv
        x = x.transpose(1, 2)
        x = self.conv1d(x)[:, :, :L]
        x = x.transpose(1, 2)
        x = F.silu(x)
        
        # SSM (selective scan)
        y = self.ssm(x)
        
        # Gate and output
        y = y * F.silu(z)
        return self.out_proj(y)
    
    def ssm(self, x):
        """Selective state-space model computation."""
        B, L, D = x.shape
        
        # Input-dependent parameters
        x_proj = self.x_proj(x)
        # ... (simplified — full implementation uses parallel scan)
        
        A = -torch.exp(self.A_log)
        
        # Sequential scan (simplified — production uses parallel scan)
        h = torch.zeros(B, D, self.A_log.shape[0], device=x.device)
        outputs = []
        for t in range(L):
            h = h * torch.exp(A) + x[:, t, :].unsqueeze(-1)
            y = (h * 1.0).sum(-1)  # Simplified output
            outputs.append(y)
        
        return torch.stack(outputs, dim=1) + x * self.D
```

---

## Next: [Production & Deployment →](07_Production.md)
