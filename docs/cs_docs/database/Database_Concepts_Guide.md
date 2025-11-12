# Database Concepts Guide - Part 1

*Comprehensive guide covering database fundamentals from basics to advanced concepts*

## Table of Contents (Part 1)
1. [Introduction to Database Systems](#introduction)
2. [Entity-Relationship (ER) Model](#er-model)
3. [Relational Model](#relational-model)
4. [Relational Algebra](#relational-algebra)
5. [Tuple Relational Calculus](#tuple-calculus)

---

## 1. Introduction to Database Systems {#introduction}

### What is a Database?

**Database**: A structured collection of data that is stored and accessed electronically from a computer system.

**Database Management System (DBMS)**: Software that interacts with users, applications, and the database itself to capture and analyze data.

### Key Characteristics of Databases
- **Data Independence**: Physical and logical data independence
- **Data Integrity**: Maintaining accuracy and consistency
- **Concurrent Access**: Multiple users accessing simultaneously
- **Security**: Controlled access to data
- **Recovery**: Backup and restore capabilities

### Database System Architecture

**Three-Level Architecture (ANSI-SPARC)**

**1. External Level (View Level)**
- **Purpose**: User's view of the database
- **Components**: Individual user views, application-specific views
- **Characteristics**: Customized, simplified, secure

**2. Conceptual Level (Logical Level)**
- **Purpose**: Community view of the database
- **Components**: Complete logical structure, relationships, constraints
- **Characteristics**: Implementation-independent, complete database view

**3. Internal Level (Physical Level)**
- **Purpose**: Physical storage of data
- **Components**: Storage structures, access paths, indexes
- **Characteristics**: Hardware-dependent, performance-oriented

### Data Independence

**Physical Data Independence**
- **Definition**: Ability to change physical storage without affecting conceptual schema
- **Examples**: Changing file organization, adding indexes, changing storage devices
- **Benefits**: Performance tuning without application changes

**Logical Data Independence**
- **Definition**: Ability to change conceptual schema without affecting external schemas
- **Examples**: Adding new attributes, creating new relationships
- **Benefits**: Database evolution without affecting applications

---

## 2. Entity-Relationship (ER) Model {#er-model}

### ER Model Components

**Entity**: A distinguishable object or concept in the real world
- **Strong Entity**: Has its own primary key
- **Weak Entity**: Depends on another entity for identification

**Attribute**: A property or characteristic of an entity
- **Simple**: Cannot be divided (e.g., Age)
- **Composite**: Can be divided into sub-parts (e.g., Name = FirstName + LastName)
- **Single-valued**: Has one value (e.g., SSN)
- **Multi-valued**: Can have multiple values (e.g., Phone numbers)
- **Derived**: Calculated from other attributes (e.g., Age from DOB)

**Relationship**: Association between two or more entities

### ER Diagram Symbols

```
Entity:          [Rectangle]
Weak Entity:     [Double Rectangle]
Attribute:       (Oval)
Key Attribute:   (Underlined Oval)
Multi-valued:    ((Double Oval))
Derived:         (Dashed Oval)
Relationship:    <Diamond>
Weak Relation:   <<Double Diamond>>
```

### Relationship Types

**By Cardinality**

**One-to-One (1:1)**
- Each entity in A relates to at most one entity in B
- Each entity in B relates to at most one entity in B
- Example: Person ↔ Passport

**One-to-Many (1:M)**
- Each entity in A can relate to many entities in B
- Each entity in B relates to at most one entity in A
- Example: Department → Employees

**Many-to-Many (M:N)**
- Each entity in A can relate to many entities in B
- Each entity in B can relate to many entities in A
- Example: Students ↔ Courses

### Participation Constraints

**Total Participation (Mandatory)**
- Every entity must participate in the relationship
- Represented by double line
- Example: Every employee must work in a department

**Partial Participation (Optional)**
- Some entities may not participate
- Represented by single line
- Example: Not all employees manage a department

### ER Design Example

**University Database**

**Entities:**
- **Student**: StudentID, Name, DOB, Address, Phone
- **Course**: CourseID, CourseName, Credits, Department
- **Professor**: ProfID, Name, Department, Salary
- **Department**: DeptID, DeptName, Location

**Relationships:**
- **Enrolls**: Student M:N Course (with Grade attribute)
- **Teaches**: Professor 1:M Course
- **Belongs**: Professor M:1 Department
- **Offers**: Department 1:M Course

### Extended ER Features

**Specialization/Generalization**

**Specialization**: Top-down approach
- Start with general entity
- Create specialized sub-entities
- Example: Employee → {Manager, Engineer, Clerk}

**Generalization**: Bottom-up approach
- Start with specific entities
- Create general super-entity
- Example: {Car, Truck, Bus} → Vehicle

**Inheritance**: Sub-entities inherit attributes from super-entity

**Constraints in Specialization**
- **Disjoint**: Entity can belong to at most one sub-class
- **Overlapping**: Entity can belong to multiple sub-classes
- **Total**: Every super-entity must belong to some sub-class
- **Partial**: Some super-entities may not belong to any sub-class

**Aggregation**
- Treat relationship as higher-level entity
- Used when relationship itself participates in another relationship
- Example: (Student, Course, Semester) → Evaluation

---

## 3. Relational Model {#relational-model}

### Relational Model Concepts

**Relation**: A table with rows and columns
- **Tuple**: A row in the relation
- **Attribute**: A column in the relation
- **Domain**: Set of allowable values for an attribute
- **Degree**: Number of attributes in a relation
- **Cardinality**: Number of tuples in a relation

### Relational Model Terminology

| Formal Term | Informal Term |
|-------------|---------------|
| Relation    | Table         |
| Tuple       | Row/Record    |
| Attribute   | Column/Field  |
| Cardinality | Number of rows|
| Degree      | Number of columns|
| Domain      | Data type     |

### Properties of Relations

**1. Atomic Values**
- Each cell contains single, indivisible value
- No repeating groups or arrays

**2. Unique Tuples**
- No two tuples are identical
- Each tuple is uniquely identifiable

**3. Unordered Tuples**
- Order of tuples doesn't matter
- Logical vs. physical ordering

**4. Unordered Attributes**
- Order of attributes doesn't matter
- Accessed by name, not position

**5. Same Domain**
- All values in a column come from same domain
- Type consistency maintained

### Keys in Relational Model

**Super Key**
- Set of attributes that uniquely identifies tuples
- May contain extra attributes
- Example: {SSN, Name, Age} for Employee

**Candidate Key**
- Minimal super key
- No proper subset is a super key
- Example: {SSN} for Employee

**Primary Key**
- Chosen candidate key
- Cannot be null (Entity Integrity)
- Uniquely identifies each tuple

**Alternate Key**
- Candidate keys not chosen as primary key
- Could serve as primary key

**Foreign Key**
- Attribute(s) referencing primary key of another relation
- Maintains referential integrity
- Can be null (unless specified otherwise)

**Composite Key**
- Primary key consisting of multiple attributes
- Example: {StudentID, CourseID} in Enrollment

### Relational Schema

**Relation Schema**: R(A₁, A₂, ..., Aₙ)
- R: Relation name
- A₁, A₂, ..., Aₙ: Attribute names

**Database Schema**: Collection of relation schemas

**Example Schema:**
```
Student(StudentID, Name, DOB, Major, GPA)
Course(CourseID, CourseName, Credits, Department)
Enrollment(StudentID, CourseID, Semester, Grade)
```

### ER to Relational Mapping

**Step 1: Map Strong Entities**
- Create relation for each strong entity
- Include all simple attributes
- Choose primary key

**Step 2: Map Weak Entities**
- Create relation including owner's primary key
- Primary key = owner's key + weak entity's partial key

**Step 3: Map 1:1 Relationships**
- **Option 1**: Merge entities into single relation
- **Option 2**: Include foreign key in either relation
- **Option 3**: Create separate relation (rarely used)

**Step 4: Map 1:M Relationships**
- Include foreign key in "many" side relation
- Include relationship attributes in "many" side

**Step 5: Map M:N Relationships**
- Create new relation
- Include primary keys of both entities as foreign keys
- Combined foreign keys form primary key
- Include relationship attributes

**Step 6: Map Multi-valued Attributes**
- Create separate relation
- Include entity's primary key + multi-valued attribute
- Primary key = entity key + multi-valued attribute

**Mapping Example:**

**ER Design:**
- Student(StudentID, Name, Phone*)
- Course(CourseID, CourseName, Credits)
- Enrolls(Student M:N Course, Grade)

**Relational Schema:**
```sql
Student(StudentID, Name)
StudentPhone(StudentID, Phone)
Course(CourseID, CourseName, Credits)
Enrollment(StudentID, CourseID, Grade)
```

---

## 4. Relational Algebra {#relational-algebra}

### Introduction to Relational Algebra

**Relational Algebra**: Procedural query language with operations on relations
- **Input**: One or more relations
- **Output**: Single relation
- **Closure Property**: Result is also a relation

### Basic Operations

**1. Selection (σ)**
- **Purpose**: Select tuples satisfying given condition
- **Notation**: σ_condition(R)
- **Example**: σ_age>25(Employee)
- **Result**: All employees older than 25

**2. Projection (π)**
- **Purpose**: Select specified attributes
- **Notation**: π_attribute_list(R)
- **Example**: π_name,salary(Employee)
- **Result**: Names and salaries of all employees
- **Note**: Eliminates duplicates

**3. Union (∪)**
- **Purpose**: Combine tuples from two relations
- **Notation**: R ∪ S
- **Condition**: R and S must be union-compatible
- **Union Compatible**: Same degree, corresponding attributes have same domain
- **Result**: All tuples from R or S (no duplicates)

**4. Set Difference (-)**
- **Purpose**: Tuples in R but not in S
- **Notation**: R - S
- **Condition**: R and S must be union-compatible
- **Example**: AllStudents - GraduatedStudents = CurrentStudents

**5. Cartesian Product (×)**
- **Purpose**: Combine every tuple of R with every tuple of S
- **Notation**: R × S
- **Result Degree**: degree(R) + degree(S)
- **Result Cardinality**: |R| × |S|
- **Usually followed by selection**

**6. Rename (ρ)**
- **Purpose**: Rename relation and/or attributes
- **Notation**: ρ_S(R) or ρ_S(A1,A2,...)(R)
- **Use**: Avoid naming conflicts in operations

### Derived Operations

**7. Intersection (∩)**
- **Definition**: R ∩ S = R - (R - S)
- **Purpose**: Common tuples in both relations
- **Condition**: Union-compatible relations

**8. Join Operations**

**Theta Join (⋈_θ)**
- **Definition**: R ⋈_θ S = σ_θ(R × S)
- **Purpose**: Combine related tuples based on condition
- **Example**: Employee ⋈_Employee.DeptID=Department.DeptID Department

**Equi-Join**
- **Special case**: Theta join with equality condition
- **Most common**: Join on foreign key relationships

**Natural Join (⋈)**
- **Definition**: Equi-join on all common attributes
- **Process**: 
  1. Find common attributes
  2. Perform equi-join on common attributes
  3. Remove duplicate columns
- **Example**: Employee ⋈ Department (on DeptID)

**Outer Joins**

**Left Outer Join (⟕)**
- Include all tuples from left relation
- Pad with nulls for unmatched tuples

**Right Outer Join (⟖)**
- Include all tuples from right relation
- Pad with nulls for unmatched tuples

**Full Outer Join (⟗)**
- Include all tuples from both relations
- Pad with nulls for unmatched tuples

**9. Division (÷)**
- **Purpose**: Find tuples in R that are related to all tuples in S
- **Notation**: R ÷ S
- **Example**: Students ÷ Courses = Students enrolled in ALL courses
- **Complex operation**: Rarely used directly

### Query Examples

**Sample Relations:**
```
Employee(EmpID, Name, DeptID, Salary)
Department(DeptID, DeptName, Location)
Project(ProjID, ProjName, Budget)
WorksOn(EmpID, ProjID, Hours)
```

**Query 1**: Find names of employees in 'IT' department
```
π_Name(σ_DeptName='IT'(Employee ⋈ Department))
```

**Query 2**: Find employees working on project 'P1'
```
π_Name(σ_ProjID='P1'(Employee ⋈ WorksOn))
```

**Query 3**: Find employees working on ALL projects
```
π_EmpID,Name(Employee) ÷ π_ProjID(Project)
```

**Query 4**: Find departments with no employees
```
π_DeptID(Department) - π_DeptID(Employee)
```

### Properties of Relational Algebra

**Commutative Operations**
- R ∪ S = S ∪ R
- R ∩ S = S ∩ R
- R ⋈ S = S ⋈ R
- R × S = S × R

**Associative Operations**
- (R ∪ S) ∪ T = R ∪ (S ∪ T)
- (R ∩ S) ∩ T = R ∩ (S ∩ T)
- (R ⋈ S) ⋈ T = R ⋈ (S ⋈ T)

**Selection Properties**
- σ_c1∧c2(R) = σ_c1(σ_c2(R))
- σ_c1∨c2(R) = σ_c1(R) ∪ σ_c2(R)

**Projection Properties**
- π_A(π_B(R)) = π_A(R) if A ⊆ B

---

## 5. Tuple Relational Calculus {#tuple-calculus}

### Introduction to Tuple Calculus

**Tuple Relational Calculus (TRC)**: Non-procedural query language
- **Declarative**: Specify what you want, not how to get it
- **Based on**: First-order predicate logic
- **Tuple Variable**: Variable that ranges over tuples of a relation

### TRC Syntax

**General Form**: {t | P(t)}
- **t**: Tuple variable
- **P(t)**: Predicate (condition) involving t
- **Result**: Set of tuples t for which P(t) is true

### TRC Components

**1. Tuple Variables**
- Variables that range over relations
- Example: t ∈ Employee (t ranges over Employee relation)

**2. Conditions**
- **Comparison**: t.attribute op value
- **Membership**: t ∈ R
- **Logical**: ∧ (AND), ∨ (OR), ¬ (NOT)

**3. Quantifiers**
- **Existential (∃)**: "There exists"
- **Universal (∀)**: "For all"

### Basic TRC Queries

**Query 1**: Find all employees
```
{t | t ∈ Employee}
```

**Query 2**: Find employees with salary > 50000
```
{t | t ∈ Employee ∧ t.Salary > 50000}
```

**Query 3**: Find names of employees in IT department
```
{t.Name | t ∈ Employee ∧ t.DeptID = 'IT'}
```

### TRC with Quantifiers

**Existential Quantifier (∃)**

**Query**: Find employees who work on some project
```
{t | t ∈ Employee ∧ ∃s(s ∈ WorksOn ∧ s.EmpID = t.EmpID)}
```

**Query**: Find employees in departments located in 'New York'
```
{t | t ∈ Employee ∧ ∃d(d ∈ Department ∧ d.DeptID = t.DeptID ∧ d.Location = 'New York')}
```

**Universal Quantifier (∀)**

**Query**: Find employees who work on ALL projects
```
{t | t ∈ Employee ∧ ∀p(p ∈ Project → ∃w(w ∈ WorksOn ∧ w.EmpID = t.EmpID ∧ w.ProjID = p.ProjID))}
```

**Query**: Find projects that have ALL employees working on them
```
{p | p ∈ Project ∧ ∀e(e ∈ Employee → ∃w(w ∈ WorksOn ∧ w.ProjID = p.ProjID ∧ w.EmpID = e.EmpID))}
```

### Complex TRC Queries

**Query**: Find employees who work on project 'P1' but not on 'P2'
```
{t | t ∈ Employee ∧ 
     ∃w1(w1 ∈ WorksOn ∧ w1.EmpID = t.EmpID ∧ w1.ProjID = 'P1') ∧
     ¬∃w2(w2 ∈ WorksOn ∧ w2.EmpID = t.EmpID ∧ w2.ProjID = 'P2')}
```

**Query**: Find departments with more than 5 employees
```
{d | d ∈ Department ∧ 
     |{e | e ∈ Employee ∧ e.DeptID = d.DeptID}| > 5}
```

### Safe Expressions

**Problem**: Some TRC expressions may produce infinite results
```
{t | ¬(t ∈ Employee)}  // All tuples NOT in Employee - infinite!
```

**Safe Expression**: TRC expression guaranteed to produce finite result

**Safety Rules**:
1. All tuple variables must be range-restricted
2. Range restriction: Variable must appear in positive relation condition
3. All free variables must be range-restricted

**Safe Version**:
```
{t | t ∈ SomeFiniteRelation ∧ ¬(t ∈ Employee)}
```

### TRC vs Relational Algebra

**Equivalence**: TRC and Relational Algebra have same expressive power
- Every TRC query can be expressed in Relational Algebra
- Every Relational Algebra query can be expressed in TRC

**Comparison**:

| Aspect | TRC | Relational Algebra |
|--------|-----|-------------------|
| Style | Declarative | Procedural |
| Approach | What to retrieve | How to retrieve |
| Complexity | Complex conditions easier | Step-by-step operations |
| Optimization | Harder to optimize | Easier to optimize |
| Learning | Mathematical background needed | More intuitive |

### TRC to SQL Translation

**TRC**: {t.Name | t ∈ Employee ∧ t.Salary > 50000}

**SQL**:
```sql
SELECT Name 
FROM Employee 
WHERE Salary > 50000;
```

**TRC with Quantifiers**:
```
{t | t ∈ Employee ∧ ∃d(d ∈ Department ∧ d.DeptID = t.DeptID ∧ d.Location = 'NY')}
```

**SQL**:
```sql
SELECT e.*
FROM Employee e
WHERE EXISTS (
    SELECT 1 
    FROM Department d 
    WHERE d.DeptID = e.DeptID AND d.Location = 'NY'
);
```

---

*Continue to Part 2 for Integrity Constraints, Normal Forms, File Organization, Indexing, Transactions, and Concurrency Control*