# 2. nn.Module & Model Building

## nn.Module Fundamentals

Every PyTorch model is a subclass of `nn.Module`. It provides:
- Parameter management (automatic tracking of learnable weights)
- Device movement (`.to(device)` moves all parameters)
- Training/eval mode (`.train()` / `.eval()`)
- Serialization (`.state_dict()` / `.load_state_dict()`)

### Basic Model

```python
import torch
import torch.nn as nn
import torch.nn.functional as F

class MLP(nn.Module):
    def __init__(self, input_dim, hidden_dim, output_dim, dropout=0.1):
        super().__init__()
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.fc2 = nn.Linear(hidden_dim, hidden_dim)
        self.fc3 = nn.Linear(hidden_dim, output_dim)
        self.dropout = nn.Dropout(dropout)
        self.norm1 = nn.LayerNorm(hidden_dim)
        self.norm2 = nn.LayerNorm(hidden_dim)
    
    def forward(self, x):
        x = self.dropout(F.gelu(self.norm1(self.fc1(x))))
        x = self.dropout(F.gelu(self.norm2(self.fc2(x))))
        return self.fc3(x)

model = MLP(784, 256, 10)
print(f"Parameters: {sum(p.numel() for p in model.parameters()):,}")
```

---

## nn.Sequential and ModuleList

```python
# Sequential (simple linear stack)
model = nn.Sequential(
    nn.Linear(784, 512),
    nn.ReLU(),
    nn.Dropout(0.2),
    nn.Linear(512, 256),
    nn.ReLU(),
    nn.Linear(256, 10)
)

# ModuleList (dynamic number of layers)
class DynamicNet(nn.Module):
    def __init__(self, num_layers, hidden_dim):
        super().__init__()
        self.layers = nn.ModuleList([
            nn.Linear(hidden_dim, hidden_dim) for _ in range(num_layers)
        ])
        self.norms = nn.ModuleList([
            nn.LayerNorm(hidden_dim) for _ in range(num_layers)
        ])
    
    def forward(self, x):
        for layer, norm in zip(self.layers, self.norms):
            x = F.gelu(norm(layer(x))) + x  # Residual
        return x

# ModuleDict (named sub-modules)
class MultiHead(nn.Module):
    def __init__(self, input_dim, heads):
        super().__init__()
        self.heads = nn.ModuleDict({
            name: nn.Linear(input_dim, dim) for name, dim in heads.items()
        })
    
    def forward(self, x):
        return {name: head(x) for name, head in self.heads.items()}
```

---

## Custom Layers

### Multi-Head Self-Attention

```python
class MultiHeadSelfAttention(nn.Module):
    def __init__(self, embed_dim, num_heads, dropout=0.0):
        super().__init__()
        self.embed_dim = embed_dim
        self.num_heads = num_heads
        self.head_dim = embed_dim // num_heads
        assert self.head_dim * num_heads == embed_dim
        
        self.qkv = nn.Linear(embed_dim, 3 * embed_dim)
        self.out_proj = nn.Linear(embed_dim, embed_dim)
        self.dropout = nn.Dropout(dropout)
        self.scale = self.head_dim ** -0.5
    
    def forward(self, x, mask=None):
        B, N, C = x.shape
        
        # Compute Q, K, V in one projection
        qkv = self.qkv(x).reshape(B, N, 3, self.num_heads, self.head_dim)
        qkv = qkv.permute(2, 0, 3, 1, 4)  # (3, B, heads, N, head_dim)
        q, k, v = qkv.unbind(0)
        
        # Scaled dot-product attention (uses FlashAttention when available)
        attn = F.scaled_dot_product_attention(
            q, k, v, attn_mask=mask, dropout_p=self.dropout.p if self.training else 0.0
        )
        
        # Reshape and project
        attn = attn.transpose(1, 2).reshape(B, N, C)
        return self.out_proj(attn)
```

### Transformer Block

```python
class TransformerBlock(nn.Module):
    def __init__(self, embed_dim, num_heads, mlp_ratio=4.0, dropout=0.1):
        super().__init__()
        self.norm1 = nn.LayerNorm(embed_dim)
        self.attn = MultiHeadSelfAttention(embed_dim, num_heads, dropout)
        self.norm2 = nn.LayerNorm(embed_dim)
        self.mlp = nn.Sequential(
            nn.Linear(embed_dim, int(embed_dim * mlp_ratio)),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(int(embed_dim * mlp_ratio), embed_dim),
            nn.Dropout(dropout),
        )
    
    def forward(self, x, mask=None):
        x = x + self.attn(self.norm1(x), mask)
        x = x + self.mlp(self.norm2(x))
        return x
```

### Vision Transformer (ViT)

```python
class VisionTransformer(nn.Module):
    """Vision Transformer for image classification."""
    
    def __init__(self, image_size=224, patch_size=16, num_classes=1000,
                 embed_dim=768, depth=12, num_heads=12, mlp_ratio=4.0):
        super().__init__()
        num_patches = (image_size // patch_size) ** 2
        
        # Patch embedding
        self.patch_embed = nn.Conv2d(3, embed_dim, kernel_size=patch_size, stride=patch_size)
        
        # Positional embedding + CLS token
        self.cls_token = nn.Parameter(torch.zeros(1, 1, embed_dim))
        self.pos_embed = nn.Parameter(torch.zeros(1, num_patches + 1, embed_dim))
        self.pos_drop = nn.Dropout(0.1)
        
        # Transformer blocks
        self.blocks = nn.Sequential(*[
            TransformerBlock(embed_dim, num_heads, mlp_ratio)
            for _ in range(depth)
        ])
        
        # Classification head
        self.norm = nn.LayerNorm(embed_dim)
        self.head = nn.Linear(embed_dim, num_classes)
        
        # Initialize
        nn.init.trunc_normal_(self.pos_embed, std=0.02)
        nn.init.trunc_normal_(self.cls_token, std=0.02)
    
    def forward(self, x):
        B = x.shape[0]
        
        # Patch embedding: (B, 3, 224, 224) → (B, num_patches, embed_dim)
        x = self.patch_embed(x).flatten(2).transpose(1, 2)
        
        # Prepend CLS token
        cls = self.cls_token.expand(B, -1, -1)
        x = torch.cat([cls, x], dim=1)
        
        # Add positional embedding
        x = self.pos_drop(x + self.pos_embed)
        
        # Transformer
        x = self.blocks(x)
        x = self.norm(x)
        
        # Classification from CLS token
        return self.head(x[:, 0])
```

---

## Parameter Management

```python
model = VisionTransformer()

# All parameters
for name, param in model.named_parameters():
    print(f"{name}: {param.shape}, requires_grad={param.requires_grad}")

# Freeze/unfreeze
for param in model.patch_embed.parameters():
    param.requires_grad = False  # Freeze patch embedding

# Parameter groups (different LR for different parts)
optimizer = torch.optim.AdamW([
    {'params': model.blocks.parameters(), 'lr': 1e-4},
    {'params': model.head.parameters(), 'lr': 1e-3},
], weight_decay=0.01)

# Save/Load
torch.save(model.state_dict(), 'model.pth')
model.load_state_dict(torch.load('model.pth', map_location=device))

# Count parameters
total = sum(p.numel() for p in model.parameters())
trainable = sum(p.numel() for p in model.parameters() if p.requires_grad)
print(f"Total: {total:,}, Trainable: {trainable:,}")
```

---

## Hooks (Inspect Internals)

```python
# Forward hook: inspect intermediate activations
activations = {}

def save_activation(name):
    def hook(module, input, output):
        activations[name] = output.detach()
    return hook

model.blocks[0].register_forward_hook(save_activation('block_0'))
model.blocks[-1].register_forward_hook(save_activation('block_last'))

output = model(input_tensor)
print(f"Block 0 output shape: {activations['block_0'].shape}")

# Backward hook: inspect/modify gradients
def gradient_hook(module, grad_input, grad_output):
    # Gradient clipping per layer
    return tuple(g.clamp(-1, 1) if g is not None else g for g in grad_input)

model.blocks[0].register_full_backward_hook(gradient_hook)
```

---

## Weight Initialization

```python
def init_weights(module):
    if isinstance(module, nn.Linear):
        nn.init.xavier_uniform_(module.weight)
        if module.bias is not None:
            nn.init.zeros_(module.bias)
    elif isinstance(module, nn.Conv2d):
        nn.init.kaiming_normal_(module.weight, mode='fan_out', nonlinearity='relu')
    elif isinstance(module, nn.LayerNorm):
        nn.init.ones_(module.weight)
        nn.init.zeros_(module.bias)
    elif isinstance(module, nn.Embedding):
        nn.init.normal_(module.weight, std=0.02)

model.apply(init_weights)
```

---

## Next: [Training & Optimization →](03_Training.md)
