# AI/ML Algorithm Theory Deep Dive

*Comprehensive theoretical analysis of machine learning algorithms, their mathematical foundations, assumptions, and theoretical properties*

## Table of Contents
1. [Supervised Learning Algorithms Theory](#supervised-algorithms)
2. [Unsupervised Learning Algorithms Theory](#unsupervised-algorithms)
3. [Deep Learning Theory](#deep-learning-theory)
4. [Ensemble Methods Theory](#ensemble-theory)
5. [Optimization Algorithms Theory](#optimization-theory)
6. [Probabilistic Models Theory](#probabilistic-theory)
7. [Kernel Methods Theory](#kernel-theory)
8. [Dimensionality Reduction Theory](#dimensionality-theory)

---

## 1. Supervised Learning Algorithms Theory {#supervised-algorithms}

### Linear Regression Theory

**Mathematical Foundation**

**The Linear Model**
```
y = β₀ + β₁x₁ + β₂x₂ + ... + βₚxₚ + ε

Where:
- y: Response variable
- β₀: Intercept (bias term)
- βᵢ: Coefficients (weights)
- xᵢ: Predictor variables (features)
- ε: Error term (noise)
```

**Matrix Formulation**
```
Y = Xβ + ε

Where:
- Y: n×1 response vector
- X: n×(p+1) design matrix
- β: (p+1)×1 coefficient vector
- ε: n×1 error vector
```

**Assumptions (LINE)**
1. **Linearity**: E[Y|X] = Xβ (linear relationship)
2. **Independence**: Observations are independent
3. **Normality**: ε ~ N(0, σ²I) (errors are normally distributed)
4. **Equal Variance**: Var(ε) = σ²I (homoscedasticity)

**Least Squares Estimation**

**Objective Function**
```
RSS(β) = ||Y - Xβ||² = (Y - Xβ)ᵀ(Y - Xβ)
```

**Normal Equations**
```
∂RSS/∂β = -2XᵀY + 2XᵀXβ = 0
β̂ = (XᵀX)⁻¹XᵀY
```

**Geometric Interpretation**
- **Projection**: ŷ = Xβ̂ is orthogonal projection of Y onto column space of X
- **Residuals**: e = Y - ŷ are orthogonal to column space of X
- **Hat Matrix**: H = X(XᵀX)⁻¹Xᵀ projects onto column space

**Statistical Properties**

**Unbiasedness**
```
E[β̂] = E[(XᵀX)⁻¹XᵀY] = (XᵀX)⁻¹XᵀE[Y] = (XᵀX)⁻¹XᵀXβ = β
```

**Variance**
```
Var(β̂) = σ²(XᵀX)⁻¹
```

**Gauss-Markov Theorem**
Under LINE assumptions, OLS estimator is BLUE (Best Linear Unbiased Estimator):
- **Best**: Minimum variance among all linear unbiased estimators
- **Linear**: Linear combination of observations
- **Unbiased**: E[β̂] = β

**Inference**

**Confidence Intervals**
```
β̂ⱼ ± t_{α/2,n-p-1} × SE(β̂ⱼ)

Where SE(β̂ⱼ) = σ̂√[(XᵀX)⁻¹]ⱼⱼ
```

**Hypothesis Testing**
```
H₀: βⱼ = 0 vs H₁: βⱼ ≠ 0
t = β̂ⱼ/SE(β̂ⱼ) ~ t_{n-p-1}
```

**Model Diagnostics**

**Residual Analysis**
- **Normality**: Q-Q plots, Shapiro-Wilk test
- **Homoscedasticity**: Residuals vs fitted plots, Breusch-Pagan test
- **Independence**: Durbin-Watson test, ACF plots
- **Linearity**: Partial residual plots

**Leverage and Influence**
- **Leverage**: hᵢᵢ = diagonal elements of hat matrix
- **Cook's Distance**: Measures influence of observation
- **DFBETAS**: Change in coefficients when observation removed

### Logistic Regression Theory

**Mathematical Foundation**

**The Logistic Model**
```
P(Y = 1|X) = π(x) = exp(β₀ + β₁x₁ + ... + βₚxₚ) / (1 + exp(β₀ + β₁x₁ + ... + βₚxₚ))
```

**Logit Transformation**
```
logit(π) = log(π/(1-π)) = β₀ + β₁x₁ + ... + βₚxₚ
```

**Odds and Odds Ratio**
- **Odds**: π/(1-π)
- **Log-odds**: log(π/(1-π)) = linear in parameters
- **Odds Ratio**: exp(βⱼ) = change in odds for unit change in xⱼ

**Maximum Likelihood Estimation**

**Likelihood Function**
```
L(β) = ∏ᵢ π(xᵢ)^yᵢ (1-π(xᵢ))^(1-yᵢ)
```

**Log-likelihood**
```
ℓ(β) = Σᵢ [yᵢ log π(xᵢ) + (1-yᵢ) log(1-π(xᵢ))]
```

**Score Function**
```
U(β) = ∂ℓ/∂β = XᵀW(Y - π)

Where W = diag(π(xᵢ)(1-π(xᵢ)))
```

**Fisher Information Matrix**
```
I(β) = XᵀWX
```

**Newton-Raphson Algorithm**
```
β^(t+1) = β^(t) + I(β^(t))⁻¹U(β^(t))
```

**Asymptotic Properties**
- **Consistency**: β̂ → β as n → ∞
- **Asymptotic Normality**: √n(β̂ - β) → N(0, I(β)⁻¹)
- **Efficiency**: Achieves Cramér-Rao lower bound

**Model Assessment**

**Deviance**
```
D = -2[ℓ(β̂) - ℓ(saturated model)]
```

**AIC/BIC**
```
AIC = -2ℓ(β̂) + 2p
BIC = -2ℓ(β̂) + p log(n)
```

**Hosmer-Lemeshow Test**
Tests goodness of fit by comparing observed and expected frequencies in groups.

### Support Vector Machine Theory

**Mathematical Foundation**

**Linear SVM (Hard Margin)**

**Optimization Problem**
```
minimize: ½||w||²
subject to: yᵢ(wᵀxᵢ + b) ≥ 1, i = 1,...,n
```

**Geometric Interpretation**
- **Hyperplane**: wᵀx + b = 0
- **Margin**: 2/||w||
- **Support Vectors**: Points on margin boundary

**Lagrangian Formulation**
```
L(w,b,α) = ½||w||² - Σᵢ αᵢ[yᵢ(wᵀxᵢ + b) - 1]
```

**KKT Conditions**
1. **Stationarity**: ∇_w L = 0, ∇_b L = 0
2. **Primal Feasibility**: yᵢ(wᵀxᵢ + b) ≥ 1
3. **Dual Feasibility**: αᵢ ≥ 0
4. **Complementary Slackness**: αᵢ[yᵢ(wᵀxᵢ + b) - 1] = 0

**Dual Problem**
```
maximize: Σᵢ αᵢ - ½ΣᵢΣⱼ αᵢαⱼyᵢyⱼxᵢᵀxⱼ
subject to: Σᵢ αᵢyᵢ = 0, αᵢ ≥ 0
```

**Solution**
```
w* = Σᵢ αᵢ*yᵢxᵢ
f(x) = sign(Σᵢ αᵢ*yᵢxᵢᵀx + b*)
```

**Soft Margin SVM**

**Optimization Problem**
```
minimize: ½||w||² + C Σᵢ ξᵢ
subject to: yᵢ(wᵀxᵢ + b) ≥ 1 - ξᵢ, ξᵢ ≥ 0
```

**Dual Problem**
```
maximize: Σᵢ αᵢ - ½ΣᵢΣⱼ αᵢαⱼyᵢyⱼxᵢᵀxⱼ
subject to: Σᵢ αᵢyᵢ = 0, 0 ≤ αᵢ ≤ C
```

**Kernel SVM**

**Kernel Trick**
Replace inner products xᵢᵀxⱼ with kernel function K(xᵢ,xⱼ) = φ(xᵢ)ᵀφ(xⱼ)

**Popular Kernels**
- **Linear**: K(x,x') = xᵀx'
- **Polynomial**: K(x,x') = (xᵀx' + c)^d
- **RBF**: K(x,x') = exp(-γ||x-x'||²)
- **Sigmoid**: K(x,x') = tanh(γxᵀx' + c)

**Mercer's Theorem**
A function K(x,x') is a valid kernel if and only if the kernel matrix is positive semidefinite for any finite set of points.

**Statistical Learning Theory**
- **VC Dimension**: For RBF kernels, can be infinite
- **Structural Risk Minimization**: C parameter controls complexity
- **Generalization Bounds**: Depend on margin and number of support vectors

### Decision Tree Theory

**Mathematical Foundation**

**Impurity Measures**

**Gini Impurity**
```
Gini(S) = 1 - Σₖ pₖ²

Where pₖ = proportion of class k in set S
```

**Entropy**
```
Entropy(S) = -Σₖ pₖ log₂(pₖ)
```

**Classification Error**
```
Error(S) = 1 - max_k pₖ
```

**Information Gain**
```
IG(S,A) = Impurity(S) - Σᵥ |Sᵥ|/|S| × Impurity(Sᵥ)

Where Sᵥ = subset of S where attribute A has value v
```

**Splitting Criteria**

**For Continuous Attributes**
- **Binary Splits**: Find threshold t that maximizes information gain
- **Optimization**: Try all possible thresholds (midpoints between consecutive values)

**For Categorical Attributes**
- **Multi-way Splits**: One branch per category
- **Binary Splits**: Subset vs complement

**Tree Construction Algorithm**

**Recursive Partitioning**
1. **Base Case**: If stopping criterion met, create leaf
2. **Find Best Split**: Maximize information gain over all attributes and thresholds
3. **Partition Data**: Split data according to best attribute
4. **Recurse**: Apply algorithm to each partition

**Stopping Criteria**
- **Pure Node**: All examples have same class
- **Maximum Depth**: Limit tree depth
- **Minimum Samples**: Don't split nodes with few examples
- **Minimum Improvement**: Only split if information gain exceeds threshold

**Pruning Theory**

**Overfitting Problem**
- **Training Error**: Decreases monotonically with tree size
- **Test Error**: Initially decreases, then increases (overfitting)
- **Bias-Variance**: Larger trees have lower bias but higher variance

**Cost-Complexity Pruning**
```
Cost(T) = Error(T) + α|T|

Where |T| = number of leaves, α = complexity parameter
```

**Pruning Algorithm**
1. **Grow Full Tree**: Until stopping criteria met
2. **Compute α**: For each internal node, find α where pruning is beneficial
3. **Prune**: Remove subtree with smallest α
4. **Repeat**: Until only root remains
5. **Select**: Use cross-validation to choose best α

**Theoretical Properties**

**Consistency**
Under mild conditions, decision trees are consistent:
- **Probability**: P(error) → Bayes error as n → ∞
- **Assumptions**: Infinite data, appropriate stopping criteria

**Bias-Variance Decomposition**
- **Bias**: Decreases with tree complexity
- **Variance**: Increases with tree complexity
- **Optimal Complexity**: Minimizes bias + variance

**Computational Complexity**
- **Training**: O(n log n × p × d) where d = depth
- **Prediction**: O(d)
- **Space**: O(number of nodes)

---

## 2. Unsupervised Learning Algorithms Theory {#unsupervised-algorithms}

### K-Means Clustering Theory

**Mathematical Foundation**

**Objective Function**
```
J = ΣᵢΣₖ wᵢₖ ||xᵢ - μₖ||²

Where:
- wᵢₖ = 1 if xᵢ assigned to cluster k, 0 otherwise
- μₖ = centroid of cluster k
```

**Lloyd's Algorithm**
1. **Initialize**: Choose k centroids randomly
2. **Assignment**: Assign each point to nearest centroid
3. **Update**: Recompute centroids as mean of assigned points
4. **Repeat**: Until convergence

**Convergence Properties**
- **Monotonic Decrease**: Objective function decreases at each iteration
- **Finite Convergence**: Algorithm terminates in finite steps
- **Local Optimum**: May converge to local minimum

**Theoretical Analysis**

**Voronoi Diagrams**
- **Partition**: Each cluster corresponds to Voronoi cell
- **Boundary**: Points equidistant from two centroids
- **Optimal Assignment**: Given centroids, Voronoi partition minimizes objective

**Centroid Update**
```
μₖ = (1/|Cₖ|) Σᵢ∈Cₖ xᵢ

Where Cₖ = set of points assigned to cluster k
```

**Initialization Strategies**
- **Random**: Choose k points randomly
- **K-means++**: Choose initial centroids to be far apart
- **Multiple Runs**: Run algorithm multiple times, choose best result

**Model Selection**

**Choosing k**
- **Elbow Method**: Plot objective vs k, look for "elbow"
- **Silhouette Analysis**: Measure cluster cohesion and separation
- **Gap Statistic**: Compare to null distribution
- **Information Criteria**: AIC, BIC for mixture models

**Assumptions and Limitations**
- **Spherical Clusters**: Assumes clusters are roughly spherical
- **Similar Sizes**: Works best when clusters have similar sizes
- **Similar Densities**: Assumes clusters have similar densities
- **Euclidean Distance**: Uses Euclidean distance metric

### Principal Component Analysis Theory

**Mathematical Foundation**

**Covariance Matrix**
```
C = (1/n) Σᵢ (xᵢ - μ)(xᵢ - μ)ᵀ = (1/n) XᵀX

Where X is centered data matrix
```

**Eigendecomposition**
```
C = VΛVᵀ

Where:
- V = matrix of eigenvectors (principal components)
- Λ = diagonal matrix of eigenvalues
```

**Principal Components**
- **First PC**: Direction of maximum variance
- **Second PC**: Direction of maximum variance orthogonal to first
- **k-th PC**: Direction of maximum variance orthogonal to first k-1

**Dimensionality Reduction**
```
Y = XVₖ

Where Vₖ = first k eigenvectors
```

**Theoretical Properties**

**Variance Maximization**
PCA finds linear combinations that maximize variance:
```
max_w Var(Xw) subject to ||w|| = 1
```

**Reconstruction Error Minimization**
PCA minimizes reconstruction error:
```
min_Vₖ ||X - XVₖVₖᵀ||²_F
```

**Eckart-Young Theorem**
PCA gives best rank-k approximation to data matrix in Frobenius norm.

**Statistical Interpretation**

**Probabilistic PCA**
Assume data generated by:
```
x = Wz + μ + ε

Where:
- W = p×k loading matrix
- z ~ N(0,I) = k-dimensional latent variable
- ε ~ N(0,σ²I) = noise
```

**Maximum Likelihood**
ML estimate of W is related to principal components:
```
Ŵ = Vₖ(Λₖ - σ²I)^(1/2)R

Where R is arbitrary orthogonal matrix
```

**Model Selection**

**Choosing Number of Components**
- **Scree Plot**: Plot eigenvalues, look for "elbow"
- **Proportion of Variance**: Choose k to explain desired percentage
- **Kaiser Rule**: Keep components with eigenvalue > 1
- **Cross-Validation**: Minimize reconstruction error on held-out data

**Computational Aspects**

**SVD Approach**
```
X = UΣVᵀ

Then C = (1/n)VΣ²Vᵀ
```

**Advantages**:
- **Numerical Stability**: More stable than eigendecomposition
- **Efficiency**: Can compute only needed components
- **Sparse Data**: Works with sparse matrices

### Hierarchical Clustering Theory

**Mathematical Foundation**

**Distance Matrix**
```
D = [dᵢⱼ] where dᵢⱼ = distance between points i and j
```

**Linkage Criteria**

**Single Linkage**
```
d(A,B) = min{d(a,b) : a ∈ A, b ∈ B}
```

**Complete Linkage**
```
d(A,B) = max{d(a,b) : a ∈ A, b ∈ B}
```

**Average Linkage**
```
d(A,B) = (1/|A||B|) Σₐ∈ₐ Σᵦ∈ᵦ d(a,b)
```

**Ward Linkage**
```
d(A,B) = √[(|A||B|)/(|A|+|B|)] ||μₐ - μᵦ||²
```

**Agglomerative Algorithm**
1. **Initialize**: Each point is its own cluster
2. **Find Closest**: Find pair of clusters with minimum distance
3. **Merge**: Combine closest clusters
4. **Update**: Recompute distances to new cluster
5. **Repeat**: Until single cluster remains

**Theoretical Properties**

**Dendrogram**
- **Tree Structure**: Represents hierarchy of clusters
- **Height**: Distance at which clusters merge
- **Cutting**: Choose number of clusters by cutting at height

**Lance-Williams Formula**
General formula for updating distances after merge:
```
d(A∪B,C) = αₐd(A,C) + αᵦd(B,C) + βd(A,B) + γ|d(A,C) - d(B,C)|
```

**Monotonicity**
Some linkage criteria are monotonic (merge distances increase), others are not.

**Space Complexity**
- **Distance Matrix**: O(n²) space
- **Memory Efficient**: Use only O(n) space with careful implementation

---

## 3. Deep Learning Theory {#deep-learning-theory}

### Neural Network Theory

**Universal Approximation Theorem**

**Statement**
Let φ be a non-constant, bounded, and continuous activation function. Then finite sums of the form:
```
F(x) = Σᵢ αᵢφ(wᵢᵀx + bᵢ)
```
are dense in C(K) for any compact set K ⊂ ℝⁿ.

**Implications**
- **Theoretical Power**: Neural networks can approximate any continuous function
- **Practical Limitations**: May need exponentially many neurons
- **Depth vs Width**: Deep networks can be more efficient

**Expressivity Theory**

**Depth vs Width Trade-offs**
- **Shallow Networks**: May need exponential width
- **Deep Networks**: Can represent same functions with polynomial width
- **Hierarchical Features**: Deep networks learn hierarchical representations

**Approximation Rates**
For smooth functions, deep networks achieve better approximation rates than shallow networks.

### Backpropagation Theory

**Mathematical Foundation**

**Forward Pass**
```
aˡ = σ(Wˡaˡ⁻¹ + bˡ)

Where:
- aˡ = activations at layer l
- Wˡ = weight matrix at layer l
- bˡ = bias vector at layer l
- σ = activation function
```

**Loss Function**
```
L = (1/2)||y - aᴸ||²  (for regression)
L = -Σᵢ yᵢ log(aᵢᴸ)  (for classification)
```

**Backward Pass**

**Output Layer Error**
```
δᴸ = ∇ₐL ⊙ σ'(zᴸ)

Where ⊙ denotes element-wise product
```

**Hidden Layer Error**
```
δˡ = ((Wˡ⁺¹)ᵀδˡ⁺¹) ⊙ σ'(zˡ)
```

**Gradient Computation**
```
∂L/∂Wˡ = δˡ(aˡ⁻¹)ᵀ
∂L/∂bˡ = δˡ
```

**Chain Rule**
Backpropagation is application of chain rule:
```
∂L/∂wᵢⱼˡ = (∂L/∂zⱼˡ)(∂zⱼˡ/∂wᵢⱼˡ) = δⱼˡaᵢˡ⁻¹
```

**Computational Complexity**
- **Forward Pass**: O(W) where W = number of weights
- **Backward Pass**: O(W) (same as forward pass)
- **Total**: O(W) per training example

### Optimization in Deep Learning

**Gradient Descent Variants**

**Stochastic Gradient Descent**
```
θₜ₊₁ = θₜ - η∇L(θₜ; xᵢ, yᵢ)
```

**Mini-batch Gradient Descent**
```
θₜ₊₁ = θₜ - η(1/|B|)Σᵢ∈B ∇L(θₜ; xᵢ, yᵢ)
```

**Momentum**
```
vₜ₊₁ = γvₜ + η∇L(θₜ)
θₜ₊₁ = θₜ - vₜ₊₁
```

**Adam**
```
mₜ₊₁ = β₁mₜ + (1-β₁)∇L(θₜ)
vₜ₊₁ = β₂vₜ + (1-β₂)(∇L(θₜ))²
θₜ₊₁ = θₜ - η(m̂ₜ₊₁)/(√v̂ₜ₊₁ + ε)
```

**Convergence Theory**

**Non-convex Optimization**
- **Local Minima**: May get stuck in suboptimal solutions
- **Saddle Points**: Points where gradient is zero but not minima
- **Landscape**: Loss surface is highly non-convex

**Generalization Theory**

**Rademacher Complexity**
Bound on generalization error in terms of model complexity.

**PAC-Bayes Bounds**
Probabilistic bounds on generalization error.

**Implicit Regularization**
SGD has implicit regularization effect, preferring solutions with certain properties.

---

## 4. Ensemble Methods Theory {#ensemble-theory}

### Bootstrap Aggregating (Bagging)

**Theoretical Foundation**

**Bootstrap Sampling**
Given dataset D = {(x₁,y₁), ..., (xₙ,yₙ)}, create bootstrap sample by sampling n examples with replacement.

**Bagging Algorithm**
1. **Generate**: B bootstrap samples D₁, D₂, ..., Dᵦ
2. **Train**: Model hᵦ on each bootstrap sample
3. **Combine**: 
   - Regression: ĥ(x) = (1/B)Σᵦ hᵦ(x)
   - Classification: ĥ(x) = majority vote

**Bias-Variance Analysis**

**Individual Model**
```
E[(h(x) - f(x))²] = Bias² + Variance + Noise
```

**Bagged Model**
If models are independent with equal bias and variance:
```
Bias_bagged = Bias_individual
Variance_bagged = Variance_individual / B
```

**Correlation Effect**
With correlation ρ between models:
```
Variance_bagged = ρ × Variance_individual + (1-ρ) × Variance_individual / B
```

**Out-of-Bag Error**
- **Definition**: Error on examples not in bootstrap sample
- **Probability**: P(example not selected) = (1-1/n)ⁿ ≈ 1/e ≈ 0.368
- **Use**: Unbiased estimate of generalization error

### Random Forests

**Algorithm Enhancement**
Random Forests = Bagging + Random Feature Selection

**Random Feature Selection**
At each split, consider only random subset of features (typically √p for classification, p/3 for regression).

**Theoretical Properties**

**Generalization Error Bound**
```
PE* ≤ ρ̄(1-s²)/s²

Where:
- PE* = generalization error
- ρ̄ = mean correlation between trees
- s = strength of individual trees
```

**Consistency**
Random Forests are consistent under mild conditions.

**Variable Importance**
- **Gini Importance**: Based on impurity decrease
- **Permutation Importance**: Based on performance decrease when variable permuted

### Boosting Theory

**AdaBoost Algorithm**

**Theoretical Foundation**
AdaBoost minimizes exponential loss:
```
L(y, f(x)) = exp(-yf(x))
```

**Algorithm**
1. **Initialize**: w₁ᵢ = 1/n for all i
2. **For t = 1 to T**:
   - Train weak learner hₜ with weights wₜ
   - Compute error: εₜ = Σᵢ wₜᵢ I(hₜ(xᵢ) ≠ yᵢ)
   - Compute coefficient: αₜ = ½ log((1-εₜ)/εₜ)
   - Update weights: wₜ₊₁,ᵢ = wₜᵢ exp(-αₜyᵢhₜ(xᵢ))
3. **Output**: H(x) = sign(Σₜ αₜhₜ(x))

**Theoretical Guarantees**

**Training Error Bound**
```
Training Error ≤ ∏ₜ 2√(εₜ(1-εₜ))
```

If εₜ ≤ ½ - γ for all t, then training error decreases exponentially.

**Generalization Bound**
```
Test Error ≤ Training Error + O(√((d log n)/n))

Where d = VC dimension of weak learner class
```

**Margin Theory**
AdaBoost maximizes the margin (confidence of prediction).

### Gradient Boosting Theory

**Functional Gradient Descent**

**Objective**
Minimize expected loss:
```
F* = argmin_F E[L(y, F(x))]
```

**Gradient Descent in Function Space**
```
Fₜ₊₁(x) = Fₜ(x) - γₜ ∂L(y,F)/∂F|_{F=Fₜ(x)}
```

**Algorithm**
1. **Initialize**: F₀(x) = argmin_γ Σᵢ L(yᵢ, γ)
2. **For t = 1 to T**:
   - Compute pseudo-residuals: rᵢₜ = -∂L(yᵢ,F)/∂F|_{F=Fₜ₋₁(xᵢ)}
   - Fit weak learner hₜ to pseudo-residuals
   - Find optimal step size: γₜ = argmin_γ Σᵢ L(yᵢ, Fₜ₋₁(xᵢ) + γhₜ(xᵢ))
   - Update: Fₜ(x) = Fₜ₋₁(x) + γₜhₜ(x)

**Loss Functions**
- **Squared Loss**: L(y,F) = ½(y-F)²
- **Absolute Loss**: L(y,F) = |y-F|
- **Huber Loss**: Robust to outliers
- **Logistic Loss**: L(y,F) = log(1 + exp(-yF))

**Regularization**
- **Shrinkage**: Fₜ(x) = Fₜ₋₁(x) + ν·γₜhₜ(x) where 0 < ν < 1
- **Subsampling**: Use random subset of data for each iteration
- **Early Stopping**: Stop when validation error increases

---

## 5. Optimization Algorithms Theory {#optimization-theory}

### Gradient Descent Theory

**Mathematical Foundation**

**Objective Function**
```
min_θ f(θ)

Where f: ℝᵈ → ℝ is differentiable
```

**Gradient Descent Update**
```
θₜ₊₁ = θₜ - η∇f(θₜ)

Where η > 0 is learning rate
```

**Convergence Analysis**

**Convex Functions**
For convex f with L-Lipschitz gradient:
```
f(θₜ) - f(θ*) ≤ ||θ₀ - θ*||²/(2ηt)
```

**Strongly Convex Functions**
For μ-strongly convex f:
```
||θₜ - θ*||² ≤ (1 - μη)ᵗ||θ₀ - θ*||²
```

**Learning Rate Selection**
- **Too Small**: Slow convergence
- **Too Large**: May diverge
- **Optimal**: η = 1/L for L-smooth functions

### Stochastic Gradient Descent Theory

**Algorithm**
```
θₜ₊₁ = θₜ - η∇fᵢₜ(θₜ)

Where iₜ is randomly selected index
```

**Convergence Analysis**

**Assumptions**
1. **Unbiased Gradient**: E[∇fᵢ(θ)] = ∇f(θ)
2. **Bounded Variance**: E[||∇fᵢ(θ) - ∇f(θ)||²] ≤ σ²

**Convergence Rate**
For convex functions:
```
E[f(θ̄ₜ)] - f(θ*) ≤ O(1/√t)

Where θ̄ₜ = (1/t)Σₛ₌₁ᵗ θₛ
```

**Advantages**
- **Computational Efficiency**: O(1) per iteration vs O(n) for batch GD
- **Memory Efficiency**: Process one example at a time
- **Online Learning**: Can handle streaming data

### Advanced Optimization Methods

**Momentum**

**Heavy Ball Method**
```
vₜ₊₁ = γvₜ + η∇f(θₜ)
θₜ₊₁ = θₜ - vₜ₊₁
```

**Nesterov Accelerated Gradient**
```
vₜ₊₁ = γvₜ + η∇f(θₜ - γvₜ)
θₜ₊₁ = θₜ - vₜ₊₁
```

**Convergence Rate**
For convex functions: O(1/t²) vs O(1/t) for standard GD.

**Adaptive Methods**

**AdaGrad**
```
Gₜ = Gₜ₋₁ + ∇f(θₜ)∇f(θₜ)ᵀ
θₜ₊₁ = θₜ - η/√(diag(Gₜ) + ε) ⊙ ∇f(θₜ)
```

**RMSprop**
```
Gₜ = γGₜ₋₁ + (1-γ)∇f(θₜ)∇f(θₜ)ᵀ
θₜ₊₁ = θₜ - η/√(diag(Gₜ) + ε) ⊙ ∇f(θₜ)
```

**Adam**
```
mₜ = β₁mₜ₋₁ + (1-β₁)∇f(θₜ)
vₜ = β₂vₜ₋₁ + (1-β₂)∇f(θₜ)²
m̂ₜ = mₜ/(1-β₁ᵗ)
v̂ₜ = vₜ/(1-β₂ᵗ)
θₜ₊₁ = θₜ - η m̂ₜ/(√v̂ₜ + ε)
```

**Theoretical Properties**
- **Regret Bounds**: Theoretical guarantees for online learning
- **Convergence**: May not converge to optimal solution in non-convex case
- **Practical Performance**: Often works well in practice

This comprehensive theoretical analysis provides the mathematical foundations and theoretical properties of major machine learning algorithms, enabling deeper understanding of their behavior, assumptions, and guarantees.