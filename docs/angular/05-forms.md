# Forms

## Template-Driven Forms

### Basic Setup

```typescript
import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <form #userForm="ngForm" (ngSubmit)="onSubmit(userForm)">
      <input name="username" [(ngModel)]="user.username" required />
      <input name="email" [(ngModel)]="user.email" type="email" required />
      <button type="submit" [disabled]="!userForm.valid">Submit</button>
    </form>
  `
})
export class UserFormComponent {
  user = { username: '', email: '' };
  
  onSubmit(form: NgForm) {
    console.log(form.value);
  }
}
```

### Validation

```typescript
@Component({
  template: `
    <form #form="ngForm">
      <input 
        name="email" 
        [(ngModel)]="email" 
        #emailField="ngModel"
        required 
        email 
      />
      <div *ngIf="emailField.invalid && emailField.touched">
        <p *ngIf="emailField.errors?.['required']">Email is required</p>
        <p *ngIf="emailField.errors?.['email']">Invalid email</p>
      </div>
    </form>
  `
})
```

## Reactive Forms

### Basic Setup

```typescript
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="userForm" (ngSubmit)="onSubmit()">
      <input formControlName="username" />
      <input formControlName="email" />
      <button type="submit" [disabled]="!userForm.valid">Submit</button>
    </form>
  `
})
export class UserFormComponent {
  userForm = this.fb.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]]
  });
  
  constructor(private fb: FormBuilder) {}
  
  onSubmit() {
    console.log(this.userForm.value);
  }
}
```

### Validation

```typescript
@Component({
  template: `
    <form [formGroup]="form">
      <input formControlName="email" />
      <div *ngIf="email.invalid && email.touched">
        <p *ngIf="email.errors?.['required']">Required</p>
        <p *ngIf="email.errors?.['email']">Invalid email</p>
      </div>
    </form>
  `
})
export class FormComponent {
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });
  
  get email() {
    return this.form.get('email')!;
  }
}
```

### Custom Validators

```typescript
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function passwordMatchValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');
    
    if (!password || !confirmPassword) {
      return null;
    }
    
    return password.value === confirmPassword.value ? null : { passwordMismatch: true };
  };
}

// Usage
this.form = this.fb.group({
  password: ['', Validators.required],
  confirmPassword: ['', Validators.required]
}, { validators: passwordMatchValidator() });
```

### Async Validators

```typescript
import { AsyncValidatorFn, AbstractControl } from '@angular/forms';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export function usernameValidator(userService: UserService): AsyncValidatorFn {
  return (control: AbstractControl) => {
    return userService.checkUsername(control.value).pipe(
      map(exists => exists ? { usernameTaken: true } : null),
      catchError(() => of(null))
    );
  };
}

// Usage
this.form = this.fb.group({
  username: ['', [Validators.required], [usernameValidator(this.userService)]]
});
```

### Form Arrays

```typescript
import { FormArray } from '@angular/forms';

@Component({
  template: `
    <form [formGroup]="form">
      <div formArrayName="skills">
        <div *ngFor="let skill of skills.controls; let i = index">
          <input [formControlName]="i" />
          <button (click)="removeSkill(i)">Remove</button>
        </div>
      </div>
      <button (click)="addSkill()">Add Skill</button>
    </form>
  `
})
export class FormComponent {
  form = this.fb.group({
    skills: this.fb.array([])
  });
  
  get skills() {
    return this.form.get('skills') as FormArray;
  }
  
  addSkill() {
    this.skills.push(this.fb.control(''));
  }
  
  removeSkill(index: number) {
    this.skills.removeAt(index);
  }
}
```

## Interview Questions

**Q: Template-driven vs Reactive forms?**
- Template-driven: Simple, less code, ngModel
- Reactive: Complex, more control, testable

**Q: What are form validators?**
- Built-in: required, email, min, max, pattern
- Custom: Sync and async validators
- Applied at control or form level

**Q: What is FormBuilder?**
- Service to create form controls
- Reduces boilerplate code
- Provides group(), control(), array() methods

**Q: What is FormArray?**
- Dynamic form controls
- Add/remove controls at runtime
- Useful for lists of items
