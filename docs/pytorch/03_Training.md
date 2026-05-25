# 3. Training & Optimization

## Production Training Loop

```python
import torch
import torch.nn as nn
from torch.cuda.amp import GradScaler, autocast
from torch.utils.data import DataLoader
from tqdm import tqdm

class Trainer:
    def __init__(self, model, optimizer, criterion, scheduler=None,
                 device='cuda', mixed_precision=True, grad_clip=1.0,
                 accumulation_steps=1):
        self.model = model.to(device)
        self.optimizer = optimizer
        self.criterion = criterion
        self.scheduler = scheduler
        self.device = device
        self.scaler = GradScaler() if mixed_precision else None
        self.grad_clip = grad_clip
        self.accumulation_steps = accumulation_steps
    
    def train_epoch(self, dataloader: DataLoader) -> dict:
        self.model.train()
        total_loss = 0.0
        correct = 0
        total = 0
        
        self.optimizer.zero_grad()
        
        for step, (x, y) in enumerate(tqdm(dataloader, desc="Training")):
            x, y = x.to(self.device), y.to(self.device)
            
            # Mixed precision forward pass
            with autocast(device_type='cuda', enabled=self.scaler is not None):
                output = self.model(x)
                loss = self.criterion(output, y) / self.accumulation_steps
            
            # Backward pass
            if self.scaler:
                self.scaler.scale(loss).backward()
            else:
                loss.backward()
            
            # Gradient accumulation
            if (step + 1) % self.accumulation_steps == 0:
                if self.scaler:
                    self.scaler.unscale_(self.optimizer)
                
                # Gradient clipping
                if self.grad_clip:
                    nn.utils.clip_grad_norm_(self.model.parameters(), self.grad_clip)
                
                if self.scaler:
                    self.scaler.step(self.optimizer)
                    self.scaler.update()
                else:
                    self.optimizer.step()
                
                self.optimizer.zero_grad()
                
                if self.scheduler:
                    self.scheduler.step()
            
            # Metrics
            total_loss += loss.item() * self.accumulation_steps
            pred = output.argmax(dim=1)
            correct += (pred == y).sum().item()
            total += y.size(0)
        
        return {
            "loss": total_loss / len(dataloader),
            "accuracy": correct / total,
            "lr": self.optimizer.param_groups[0]['lr']
        }
    
    @torch.no_grad()
    def evaluate(self, dataloader: DataLoader) -> dict:
        self.model.eval()
        total_loss = 0.0
        correct = 0
        total = 0
        
        for x, y in dataloader:
            x, y = x.to(self.device), y.to(self.device)
            
            with autocast(device_type='cuda', enabled=self.scaler is not None):
                output = self.model(x)
                loss = self.criterion(output, y)
            
            total_loss += loss.item()
            pred = output.argmax(dim=1)
            correct += (pred == y).sum().item()
            total += y.size(0)
        
        return {"loss": total_loss / len(dataloader), "accuracy": correct / total}
    
    def fit(self, train_loader, val_loader, epochs, save_path='best_model.pth'):
        best_val_loss = float('inf')
        
        for epoch in range(epochs):
            train_metrics = self.train_epoch(train_loader)
            val_metrics = self.evaluate(val_loader)
            
            print(f"Epoch {epoch+1}/{epochs} | "
                  f"Train Loss: {train_metrics['loss']:.4f}, Acc: {train_metrics['accuracy']:.4f} | "
                  f"Val Loss: {val_metrics['loss']:.4f}, Acc: {val_metrics['accuracy']:.4f} | "
                  f"LR: {train_metrics['lr']:.6f}")
            
            if val_metrics['loss'] < best_val_loss:
                best_val_loss = val_metrics['loss']
                torch.save(self.model.state_dict(), save_path)
                print(f"  → Saved best model (val_loss={best_val_loss:.4f})")
```

---

## Optimizers

```python
# AdamW (recommended default for most tasks)
optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3, weight_decay=0.01, betas=(0.9, 0.999))

# SGD with momentum (for fine-tuning, large batch)
optimizer = torch.optim.SGD(model.parameters(), lr=0.1, momentum=0.9, nesterov=True, weight_decay=1e-4)

# 8-bit Adam (memory efficient for large models)
import bitsandbytes as bnb
optimizer = bnb.optim.Adam8bit(model.parameters(), lr=1e-4)

# Parameter groups (different LR per layer)
optimizer = torch.optim.AdamW([
    {'params': model.backbone.parameters(), 'lr': 1e-5},      # Lower LR for pretrained
    {'params': model.head.parameters(), 'lr': 1e-3},          # Higher LR for new head
], weight_decay=0.01)
```

---

## Learning Rate Schedulers

```python
from torch.optim.lr_scheduler import (
    CosineAnnealingLR, OneCycleLR, CosineAnnealingWarmRestarts,
    LinearLR, SequentialLR
)

# Cosine annealing (most common)
scheduler = CosineAnnealingLR(optimizer, T_max=num_epochs, eta_min=1e-6)

# One-cycle (fast convergence)
scheduler = OneCycleLR(
    optimizer, max_lr=1e-3, total_steps=num_epochs * len(train_loader),
    pct_start=0.1, anneal_strategy='cos'
)

# Warmup + Cosine decay (Transformer standard)
warmup = LinearLR(optimizer, start_factor=0.01, total_iters=warmup_steps)
cosine = CosineAnnealingLR(optimizer, T_max=total_steps - warmup_steps)
scheduler = SequentialLR(optimizer, schedulers=[warmup, cosine], milestones=[warmup_steps])

# Custom warmup cosine
class WarmupCosineScheduler:
    def __init__(self, optimizer, warmup_steps, total_steps, min_lr=1e-6):
        self.optimizer = optimizer
        self.warmup_steps = warmup_steps
        self.total_steps = total_steps
        self.base_lr = optimizer.param_groups[0]['lr']
        self.min_lr = min_lr
        self.step_count = 0
    
    def step(self):
        self.step_count += 1
        if self.step_count < self.warmup_steps:
            lr = self.base_lr * self.step_count / self.warmup_steps
        else:
            progress = (self.step_count - self.warmup_steps) / (self.total_steps - self.warmup_steps)
            lr = self.min_lr + 0.5 * (self.base_lr - self.min_lr) * (1 + math.cos(math.pi * progress))
        
        for param_group in self.optimizer.param_groups:
            param_group['lr'] = lr
```

---

## Mixed Precision Training (AMP)

```python
from torch.cuda.amp import autocast, GradScaler

scaler = GradScaler()

for x, y in dataloader:
    optimizer.zero_grad()
    
    # Forward pass in float16
    with autocast(device_type='cuda'):
        output = model(x)
        loss = criterion(output, y)
    
    # Backward pass with loss scaling
    scaler.scale(loss).backward()
    
    # Unscale before gradient clipping
    scaler.unscale_(optimizer)
    nn.utils.clip_grad_norm_(model.parameters(), 1.0)
    
    # Optimizer step with scaler
    scaler.step(optimizer)
    scaler.update()

# BFloat16 (better for training, no loss scaling needed)
with autocast(device_type='cuda', dtype=torch.bfloat16):
    output = model(x)
    loss = criterion(output, y)
loss.backward()
optimizer.step()
```

---

## Distributed Data Parallel (DDP)

```python
import torch.distributed as dist
from torch.nn.parallel import DistributedDataParallel as DDP
from torch.utils.data.distributed import DistributedSampler

def setup(rank, world_size):
    dist.init_process_group("nccl", rank=rank, world_size=world_size)
    torch.cuda.set_device(rank)

def cleanup():
    dist.destroy_process_group()

def train_ddp(rank, world_size, epochs):
    setup(rank, world_size)
    
    model = MyModel().to(rank)
    model = DDP(model, device_ids=[rank])
    
    sampler = DistributedSampler(dataset, num_replicas=world_size, rank=rank)
    dataloader = DataLoader(dataset, batch_size=64, sampler=sampler, pin_memory=True)
    
    optimizer = torch.optim.AdamW(model.parameters(), lr=1e-3)
    
    for epoch in range(epochs):
        sampler.set_epoch(epoch)  # Shuffle differently each epoch
        for x, y in dataloader:
            x, y = x.to(rank), y.to(rank)
            loss = model(x, y)
            loss.backward()
            optimizer.step()
            optimizer.zero_grad()
    
    cleanup()

# Launch
import torch.multiprocessing as mp
mp.spawn(train_ddp, args=(world_size, epochs), nprocs=world_size)
```

### Fully Sharded Data Parallel (FSDP)

```python
from torch.distributed.fsdp import FullyShardedDataParallel as FSDP
from torch.distributed.fsdp import MixedPrecision, ShardingStrategy

# Shard model across GPUs (for models that don't fit on one GPU)
fsdp_model = FSDP(
    model,
    sharding_strategy=ShardingStrategy.FULL_SHARD,
    mixed_precision=MixedPrecision(
        param_dtype=torch.bfloat16,
        reduce_dtype=torch.bfloat16,
        buffer_dtype=torch.bfloat16,
    ),
    device_id=torch.cuda.current_device(),
    use_orig_params=True,  # Required for torch.compile compatibility
)

# Training is same as DDP
for x, y in dataloader:
    loss = fsdp_model(x, y)
    loss.backward()
    optimizer.step()
    optimizer.zero_grad()
```

---

## Checkpointing

```python
# Save checkpoint (full state for resuming)
checkpoint = {
    'epoch': epoch,
    'model_state_dict': model.state_dict(),
    'optimizer_state_dict': optimizer.state_dict(),
    'scheduler_state_dict': scheduler.state_dict(),
    'scaler_state_dict': scaler.state_dict(),
    'best_val_loss': best_val_loss,
    'config': config,
}
torch.save(checkpoint, f'checkpoints/epoch_{epoch}.pt')

# Resume from checkpoint
checkpoint = torch.load('checkpoints/epoch_10.pt', map_location=device)
model.load_state_dict(checkpoint['model_state_dict'])
optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
scheduler.load_state_dict(checkpoint['scheduler_state_dict'])
start_epoch = checkpoint['epoch'] + 1
```

---

## Next: [Computer Vision →](04_Computer_Vision.md)
