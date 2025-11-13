# Computer Vision Theory

## Image Processing Fundamentals

### Digital Images
- **Representation**: 2D/3D arrays of pixel intensities
- **Color Spaces**: RGB, HSV, LAB, YUV
- **Sampling**: Continuous → discrete conversion
- **Quantization**: Intensity level discretization

### Convolution Theory
```
(f * g)(x,y) = ∑∑ f(m,n)g(x-m, y-n)
```

**Properties**:
- **Linearity**: (af + bg) * h = a(f*h) + b(g*h)
- **Commutativity**: f * g = g * f
- **Associativity**: (f * g) * h = f * (g * h)

### Filters and Kernels

**Edge Detection**:
- **Sobel**: Gradient approximation
- **Canny**: Multi-stage edge detection
- **Laplacian**: Second derivative operator

**Smoothing**:
- **Gaussian**: σ controls smoothing strength
- **Bilateral**: Edge-preserving smoothing
- **Median**: Noise reduction

## Feature Detection Theory

### Corner Detection

**Harris Corner Detector**
```
M = ∑(w) [Ix² IxIy; IxIy Iy²]
R = det(M) - k(trace(M))²
```

**SIFT (Scale-Invariant Feature Transform)**
- **Scale Space**: L(x,y,σ) = G(x,y,σ) * I(x,y)
- **DoG**: D(x,y,σ) = L(x,y,kσ) - L(x,y,σ)
- **Keypoint Localization**: Extrema in DoG pyramid
- **Descriptor**: 128-dimensional histogram

### Object Detection Theory

**Sliding Window**
- **Exhaustive Search**: All positions and scales
- **Computational Cost**: O(WHN) for W×H image, N scales

**Region Proposals**
- **Selective Search**: Hierarchical grouping
- **EdgeBoxes**: Edge-based proposals
- **RPN**: Learned region proposals

**R-CNN Family**
- **R-CNN**: CNN features + SVM classifier
- **Fast R-CNN**: End-to-end training
- **Faster R-CNN**: Integrated RPN

**YOLO (You Only Look Once)**
```
P(Object) × IOU × P(Class|Object)
```

**Single-shot detection**: Direct bounding box regression

## Deep Learning for Vision

### CNN Architecture Theory

**Receptive Field**
```
RF_l = RF_{l-1} + (kernel_size - 1) × stride_product
```

**Translation Equivariance**
```
f(T_v(x)) = T_v(f(x))
```

**Pooling Operations**
- **Max Pooling**: Translation invariance
- **Average Pooling**: Smooth downsampling
- **Global Average Pooling**: Spatial dimension reduction

### Advanced Architectures

**ResNet Theory**
```
H(x) = F(x) + x
```
- **Skip Connections**: Gradient flow improvement
- **Identity Mapping**: Easier optimization

**DenseNet**
```
x_l = H_l([x_0, x_1, ..., x_{l-1}])
```
- **Dense Connectivity**: Feature reuse
- **Parameter Efficiency**: Reduced redundancy

**Attention in Vision**
- **Spatial Attention**: Where to look
- **Channel Attention**: What features to emphasize
- **Self-Attention**: Long-range dependencies

## Segmentation Theory

### Semantic Segmentation
- **Pixel-wise Classification**: Each pixel gets class label
- **Fully Convolutional Networks**: No fully connected layers
- **Upsampling**: Transposed convolution, bilinear interpolation

### Instance Segmentation
- **Mask R-CNN**: Object detection + segmentation
- **ROI Align**: Precise feature extraction
- **Binary Masks**: Per-instance segmentation

### Loss Functions
- **Cross-Entropy**: Standard classification loss
- **Dice Loss**: Overlap-based loss
- **Focal Loss**: Addresses class imbalance
- **IoU Loss**: Direct optimization of IoU metric

## Generative Models for Vision

### GANs for Images
- **DCGAN**: Deep convolutional architecture
- **StyleGAN**: Style-based generation
- **Progressive GAN**: Gradual resolution increase

### VAEs for Images
- **Latent Space**: Continuous representation
- **Reconstruction Loss**: Pixel-wise similarity
- **KL Divergence**: Regularization term

This covers the essential theoretical foundations of computer vision.