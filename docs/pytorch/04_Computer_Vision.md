# 4. Computer Vision

## torchvision Models & Transforms

```python
import torch
import torchvision
from torchvision import transforms, models
from torchvision.transforms import v2  # New transforms API (2.x)

# V2 Transforms (recommended — works on images, bboxes, masks, videos)
train_transform = v2.Compose([
    v2.RandomResizedCrop(224, scale=(0.8, 1.0)),
    v2.RandomHorizontalFlip(),
    v2.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
    v2.RandAugment(num_ops=2, magnitude=9),
    v2.ToImage(),
    v2.ToDtype(torch.float32, scale=True),
    v2.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
])

val_transform = v2.Compose([
    v2.Resize(256),
    v2.CenterCrop(224),
    v2.ToImage(),
    v2.ToDtype(torch.float32, scale=True),
    v2.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
])
```

---

## Transfer Learning

```python
from torchvision.models import efficientnet_v2_s, EfficientNet_V2_S_Weights

class TransferModel(nn.Module):
    def __init__(self, num_classes, freeze_backbone=True):
        super().__init__()
        # Load pretrained backbone
        weights = EfficientNet_V2_S_Weights.DEFAULT
        self.backbone = efficientnet_v2_s(weights=weights)
        
        # Freeze backbone
        if freeze_backbone:
            for param in self.backbone.parameters():
                param.requires_grad = False
        
        # Replace classifier
        in_features = self.backbone.classifier[1].in_features
        self.backbone.classifier = nn.Sequential(
            nn.Dropout(0.3),
            nn.Linear(in_features, 512),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(512, num_classes),
        )
    
    def forward(self, x):
        return self.backbone(x)
    
    def unfreeze(self, num_layers=30):
        """Unfreeze last N layers for fine-tuning."""
        layers = list(self.backbone.features.parameters())
        for param in layers[-num_layers:]:
            param.requires_grad = True

# Two-stage training
model = TransferModel(num_classes=100, freeze_backbone=True)

# Stage 1: Train head
optimizer = torch.optim.Adam(model.backbone.classifier.parameters(), lr=1e-3)
train(model, optimizer, epochs=5)

# Stage 2: Fine-tune backbone
model.unfreeze(num_layers=50)
optimizer = torch.optim.Adam([
    {'params': model.backbone.features.parameters(), 'lr': 1e-5},
    {'params': model.backbone.classifier.parameters(), 'lr': 1e-4},
], weight_decay=0.01)
train(model, optimizer, epochs=20)
```

---

## Object Detection (DETR-style)

```python
class SimpleDETR(nn.Module):
    """Simplified Detection Transformer."""
    
    def __init__(self, num_classes, num_queries=100, hidden_dim=256):
        super().__init__()
        # CNN backbone
        backbone = torchvision.models.resnet50(weights='DEFAULT')
        self.backbone = nn.Sequential(*list(backbone.children())[:-2])
        
        # Project backbone features
        self.input_proj = nn.Conv2d(2048, hidden_dim, 1)
        
        # Transformer
        self.transformer = nn.Transformer(
            d_model=hidden_dim, nhead=8, num_encoder_layers=6,
            num_decoder_layers=6, dim_feedforward=1024, dropout=0.1
        )
        
        # Object queries (learnable)
        self.query_embed = nn.Embedding(num_queries, hidden_dim)
        
        # Prediction heads
        self.class_head = nn.Linear(hidden_dim, num_classes + 1)  # +1 for "no object"
        self.bbox_head = nn.Sequential(
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, 4),  # (cx, cy, w, h) normalized
            nn.Sigmoid(),
        )
        
        # Positional encoding
        self.pos_encoding = PositionalEncoding2D(hidden_dim)
    
    def forward(self, images):
        # Backbone features
        features = self.backbone(images)  # (B, 2048, H/32, W/32)
        features = self.input_proj(features)  # (B, hidden_dim, H', W')
        
        B, C, H, W = features.shape
        
        # Flatten spatial dims + add positional encoding
        pos = self.pos_encoding(features)
        src = features.flatten(2).permute(2, 0, 1)  # (H'*W', B, C)
        pos = pos.flatten(2).permute(2, 0, 1)
        
        # Object queries
        queries = self.query_embed.weight.unsqueeze(1).expand(-1, B, -1)
        
        # Transformer
        output = self.transformer(src + pos, queries)  # (num_queries, B, hidden_dim)
        output = output.permute(1, 0, 2)  # (B, num_queries, hidden_dim)
        
        # Predictions
        class_logits = self.class_head(output)  # (B, num_queries, num_classes+1)
        bbox_pred = self.bbox_head(output)      # (B, num_queries, 4)
        
        return {"pred_logits": class_logits, "pred_boxes": bbox_pred}
```

---

## Semantic Segmentation

```python
class UNet(nn.Module):
    def __init__(self, in_channels=3, num_classes=21):
        super().__init__()
        
        def conv_block(in_c, out_c):
            return nn.Sequential(
                nn.Conv2d(in_c, out_c, 3, padding=1), nn.BatchNorm2d(out_c), nn.ReLU(inplace=True),
                nn.Conv2d(out_c, out_c, 3, padding=1), nn.BatchNorm2d(out_c), nn.ReLU(inplace=True),
            )
        
        # Encoder
        self.enc1 = conv_block(in_channels, 64)
        self.enc2 = conv_block(64, 128)
        self.enc3 = conv_block(128, 256)
        self.enc4 = conv_block(256, 512)
        self.pool = nn.MaxPool2d(2)
        
        # Bottleneck
        self.bottleneck = conv_block(512, 1024)
        
        # Decoder
        self.up4 = nn.ConvTranspose2d(1024, 512, 2, stride=2)
        self.dec4 = conv_block(1024, 512)
        self.up3 = nn.ConvTranspose2d(512, 256, 2, stride=2)
        self.dec3 = conv_block(512, 256)
        self.up2 = nn.ConvTranspose2d(256, 128, 2, stride=2)
        self.dec2 = conv_block(256, 128)
        self.up1 = nn.ConvTranspose2d(128, 64, 2, stride=2)
        self.dec1 = conv_block(128, 64)
        
        self.final = nn.Conv2d(64, num_classes, 1)
    
    def forward(self, x):
        # Encoder
        e1 = self.enc1(x)
        e2 = self.enc2(self.pool(e1))
        e3 = self.enc3(self.pool(e2))
        e4 = self.enc4(self.pool(e3))
        
        # Bottleneck
        b = self.bottleneck(self.pool(e4))
        
        # Decoder with skip connections
        d4 = self.dec4(torch.cat([self.up4(b), e4], dim=1))
        d3 = self.dec3(torch.cat([self.up3(d4), e3], dim=1))
        d2 = self.dec2(torch.cat([self.up2(d3), e2], dim=1))
        d1 = self.dec1(torch.cat([self.up1(d2), e1], dim=1))
        
        return self.final(d1)
```

---

## Next: [NLP & Transformers →](05_NLP_Transformers.md)
