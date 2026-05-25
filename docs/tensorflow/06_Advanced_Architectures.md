# 6. Advanced Architectures

## Generative Adversarial Networks (GANs)

### Theory

```
┌──────────────┐         ┌──────────────┐
│  Generator   │────────►│Discriminator │
│  G(z) → x'  │  fake   │  D(x) → [0,1]│
└──────────────┘         └──────┬───────┘
       ▲                        │
       │                        ▼
  Noise z              Real data x
  (latent space)       (training set)

G tries to fool D (generate realistic samples)
D tries to distinguish real from fake
Minimax game: min_G max_D E[log D(x)] + E[log(1 - D(G(z)))]
```

### DCGAN Implementation

```python
class Generator(keras.Model):
    def __init__(self, latent_dim=128, **kwargs):
        super().__init__(**kwargs)
        self.dense = layers.Dense(8 * 8 * 256, use_bias=False)
        self.reshape = layers.Reshape((8, 8, 256))
        
        self.blocks = keras.Sequential([
            layers.Conv2DTranspose(128, 4, strides=2, padding='same', use_bias=False),
            layers.BatchNormalization(),
            layers.LeakyReLU(0.2),
            
            layers.Conv2DTranspose(64, 4, strides=2, padding='same', use_bias=False),
            layers.BatchNormalization(),
            layers.LeakyReLU(0.2),
            
            layers.Conv2DTranspose(32, 4, strides=2, padding='same', use_bias=False),
            layers.BatchNormalization(),
            layers.LeakyReLU(0.2),
            
            layers.Conv2D(3, 3, padding='same', activation='tanh'),  # Output: 64×64×3
        ])
    
    def call(self, z, training=False):
        x = self.dense(z)
        x = self.reshape(x)
        return self.blocks(x, training=training)


class Discriminator(keras.Model):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.blocks = keras.Sequential([
            layers.Conv2D(64, 4, strides=2, padding='same'),
            layers.LeakyReLU(0.2),
            
            layers.Conv2D(128, 4, strides=2, padding='same'),
            layers.LayerNormalization(),
            layers.LeakyReLU(0.2),
            
            layers.Conv2D(256, 4, strides=2, padding='same'),
            layers.LayerNormalization(),
            layers.LeakyReLU(0.2),
            
            layers.Flatten(),
            layers.Dense(1)  # No sigmoid (use logits for WGAN-GP)
        ])
    
    def call(self, x, training=False):
        return self.blocks(x, training=training)


class WGAN_GP(keras.Model):
    """Wasserstein GAN with Gradient Penalty — stable training."""
    
    def __init__(self, latent_dim=128, gp_weight=10.0, **kwargs):
        super().__init__(**kwargs)
        self.generator = Generator(latent_dim)
        self.discriminator = Discriminator()
        self.latent_dim = latent_dim
        self.gp_weight = gp_weight
        self.d_steps = 5  # Train D more than G
    
    def compile(self, g_optimizer, d_optimizer):
        super().compile()
        self.g_optimizer = g_optimizer
        self.d_optimizer = d_optimizer
        self.g_loss_metric = keras.metrics.Mean(name="g_loss")
        self.d_loss_metric = keras.metrics.Mean(name="d_loss")
    
    def gradient_penalty(self, real, fake):
        batch_size = tf.shape(real)[0]
        alpha = tf.random.uniform([batch_size, 1, 1, 1], 0.0, 1.0)
        interpolated = alpha * real + (1 - alpha) * fake
        
        with tf.GradientTape() as tape:
            tape.watch(interpolated)
            pred = self.discriminator(interpolated, training=True)
        
        grads = tape.gradient(pred, interpolated)
        norm = tf.sqrt(tf.reduce_sum(tf.square(grads), axis=[1, 2, 3]))
        return tf.reduce_mean((norm - 1.0) ** 2)
    
    def train_step(self, real_images):
        batch_size = tf.shape(real_images)[0]
        
        # Train Discriminator
        for _ in range(self.d_steps):
            noise = tf.random.normal([batch_size, self.latent_dim])
            with tf.GradientTape() as tape:
                fake_images = self.generator(noise, training=True)
                real_pred = self.discriminator(real_images, training=True)
                fake_pred = self.discriminator(fake_images, training=True)
                
                d_loss = tf.reduce_mean(fake_pred) - tf.reduce_mean(real_pred)
                gp = self.gradient_penalty(real_images, fake_images)
                d_loss += self.gp_weight * gp
            
            d_grads = tape.gradient(d_loss, self.discriminator.trainable_variables)
            self.d_optimizer.apply_gradients(zip(d_grads, self.discriminator.trainable_variables))
        
        # Train Generator
        noise = tf.random.normal([batch_size, self.latent_dim])
        with tf.GradientTape() as tape:
            fake_images = self.generator(noise, training=True)
            fake_pred = self.discriminator(fake_images, training=True)
            g_loss = -tf.reduce_mean(fake_pred)
        
        g_grads = tape.gradient(g_loss, self.generator.trainable_variables)
        self.g_optimizer.apply_gradients(zip(g_grads, self.generator.trainable_variables))
        
        self.g_loss_metric.update_state(g_loss)
        self.d_loss_metric.update_state(d_loss)
        return {"g_loss": self.g_loss_metric.result(), "d_loss": self.d_loss_metric.result()}
```

---

## Variational Autoencoder (VAE)

### Theory

```
Encoder: x → q(z|x) = N(μ, σ²)     (approximate posterior)
Decoder: z → p(x|z)                  (likelihood)

Loss = Reconstruction Loss + KL Divergence
     = E[log p(x|z)] - KL(q(z|x) || p(z))
     = MSE(x, x') + 0.5 * Σ(μ² + σ² - log(σ²) - 1)
```

```python
class VAE(keras.Model):
    def __init__(self, latent_dim=64, **kwargs):
        super().__init__(**kwargs)
        self.latent_dim = latent_dim
        
        # Encoder
        self.encoder = keras.Sequential([
            layers.Conv2D(32, 3, strides=2, padding='same', activation='relu'),
            layers.Conv2D(64, 3, strides=2, padding='same', activation='relu'),
            layers.Conv2D(128, 3, strides=2, padding='same', activation='relu'),
            layers.Flatten(),
        ])
        self.z_mean = layers.Dense(latent_dim)
        self.z_log_var = layers.Dense(latent_dim)
        
        # Decoder
        self.decoder = keras.Sequential([
            layers.Dense(8 * 8 * 128, activation='relu'),
            layers.Reshape((8, 8, 128)),
            layers.Conv2DTranspose(64, 3, strides=2, padding='same', activation='relu'),
            layers.Conv2DTranspose(32, 3, strides=2, padding='same', activation='relu'),
            layers.Conv2DTranspose(3, 3, strides=2, padding='same', activation='sigmoid'),
        ])
    
    def encode(self, x):
        h = self.encoder(x)
        return self.z_mean(h), self.z_log_var(h)
    
    def reparameterize(self, mean, log_var):
        eps = tf.random.normal(shape=tf.shape(mean))
        return mean + tf.exp(0.5 * log_var) * eps
    
    def decode(self, z):
        return self.decoder(z)
    
    def call(self, x, training=False):
        mean, log_var = self.encode(x)
        z = self.reparameterize(mean, log_var)
        return self.decode(z)
    
    def train_step(self, data):
        with tf.GradientTape() as tape:
            mean, log_var = self.encode(data)
            z = self.reparameterize(mean, log_var)
            reconstruction = self.decode(z)
            
            recon_loss = tf.reduce_mean(tf.reduce_sum(tf.square(data - reconstruction), axis=[1, 2, 3]))
            kl_loss = -0.5 * tf.reduce_mean(tf.reduce_sum(1 + log_var - tf.square(mean) - tf.exp(log_var), axis=1))
            total_loss = recon_loss + kl_loss
        
        grads = tape.gradient(total_loss, self.trainable_variables)
        self.optimizer.apply_gradients(zip(grads, self.trainable_variables))
        return {"loss": total_loss, "recon_loss": recon_loss, "kl_loss": kl_loss}
```

---

## Denoising Diffusion (DDPM)

### Theory

```
Forward process: Gradually add noise to data
  x_t = √(ᾱ_t) * x_0 + √(1 - ᾱ_t) * ε,  ε ~ N(0, I)

Reverse process: Learn to denoise
  Model predicts noise ε_θ(x_t, t)
  
Loss: ||ε - ε_θ(x_t, t)||²  (simple MSE on predicted noise)
```

```python
class DiffusionModel(keras.Model):
    """Simplified DDPM implementation."""
    
    def __init__(self, image_size=64, timesteps=1000, **kwargs):
        super().__init__(**kwargs)
        self.timesteps = timesteps
        self.image_size = image_size
        
        # Noise schedule (linear)
        beta = tf.linspace(1e-4, 0.02, timesteps)
        alpha = 1.0 - beta
        alpha_bar = tf.math.cumprod(alpha)
        
        self.beta = beta
        self.alpha_bar = alpha_bar
        self.sqrt_alpha_bar = tf.sqrt(alpha_bar)
        self.sqrt_one_minus_alpha_bar = tf.sqrt(1.0 - alpha_bar)
        
        # U-Net noise predictor
        self.noise_predictor = self._build_unet()
    
    def _build_unet(self):
        """Simplified U-Net for noise prediction."""
        inputs = keras.Input(shape=(self.image_size, self.image_size, 3))
        time_input = keras.Input(shape=(1,))
        
        # Time embedding
        t_emb = layers.Dense(128, activation='swish')(time_input)
        t_emb = layers.Dense(128)(t_emb)
        
        # Encoder
        x = layers.Conv2D(64, 3, padding='same', activation='swish')(inputs)
        x = layers.Conv2D(64, 3, padding='same', activation='swish')(x)
        skip1 = x
        x = layers.MaxPooling2D()(x)
        
        x = layers.Conv2D(128, 3, padding='same', activation='swish')(x)
        skip2 = x
        x = layers.MaxPooling2D()(x)
        
        # Bottleneck + time conditioning
        x = layers.Conv2D(256, 3, padding='same', activation='swish')(x)
        t = layers.Dense(256)(t_emb)
        x = x + layers.Reshape((1, 1, 256))(t)
        
        # Decoder
        x = layers.UpSampling2D()(x)
        x = layers.Concatenate()([x, skip2])
        x = layers.Conv2D(128, 3, padding='same', activation='swish')(x)
        
        x = layers.UpSampling2D()(x)
        x = layers.Concatenate()([x, skip1])
        x = layers.Conv2D(64, 3, padding='same', activation='swish')(x)
        
        outputs = layers.Conv2D(3, 1)(x)  # Predict noise
        
        return keras.Model([inputs, time_input], outputs)
    
    def train_step(self, images):
        batch_size = tf.shape(images)[0]
        
        # Sample random timesteps
        t = tf.random.uniform([batch_size], 0, self.timesteps, dtype=tf.int32)
        
        # Add noise
        noise = tf.random.normal(tf.shape(images))
        sqrt_ab = tf.gather(self.sqrt_alpha_bar, t)[:, None, None, None]
        sqrt_omab = tf.gather(self.sqrt_one_minus_alpha_bar, t)[:, None, None, None]
        noisy_images = sqrt_ab * images + sqrt_omab * noise
        
        # Predict noise
        t_normalized = tf.cast(t, tf.float32)[:, None] / self.timesteps
        
        with tf.GradientTape() as tape:
            predicted_noise = self.noise_predictor([noisy_images, t_normalized], training=True)
            loss = tf.reduce_mean(tf.square(noise - predicted_noise))
        
        grads = tape.gradient(loss, self.noise_predictor.trainable_variables)
        self.optimizer.apply_gradients(zip(grads, self.noise_predictor.trainable_variables))
        return {"loss": loss}
    
    @tf.function
    def sample(self, num_samples=16):
        """Generate images by iterative denoising."""
        x = tf.random.normal([num_samples, self.image_size, self.image_size, 3])
        
        for t in reversed(range(self.timesteps)):
            t_batch = tf.fill([num_samples, 1], t / self.timesteps)
            predicted_noise = self.noise_predictor([x, t_batch], training=False)
            
            alpha = 1.0 - self.beta[t]
            alpha_bar = self.alpha_bar[t]
            
            x = (1 / tf.sqrt(alpha)) * (x - (self.beta[t] / tf.sqrt(1 - alpha_bar)) * predicted_noise)
            
            if t > 0:
                noise = tf.random.normal(tf.shape(x))
                x += tf.sqrt(self.beta[t]) * noise
        
        return tf.clip_by_value(x, -1.0, 1.0)
```

---

## Next: [Production & Deployment →](07_Production.md)
