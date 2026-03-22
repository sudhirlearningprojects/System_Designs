# Data Binding and Directives

## Data Binding

Angular provides four types of data binding.

### 1. Interpolation (One-way: Component → View)

```typescript
@Component({
  template: `
    <h1>{{ title }}</h1>
    <p>{{ 1 + 1 }}</p>
    <p>{{ getFullName() }}</p>
  `
})
export class AppComponent {
  title = 'My App';
  firstName = 'John';
  lastName = 'Doe';
  
  getFullName() {
    return `${this.firstName} ${this.lastName}`;
  }
}
```

### 2. Property Binding (One-way: Component → View)

```typescript
@Component({
  template: `
    <img [src]="imageUrl" />
    <button [disabled]="isDisabled">Click</button>
    <div [class.active]="isActive"></div>
    <div [style.color]="textColor"></div>
  `
})
export class AppComponent {
  imageUrl = 'assets/logo.png';
  isDisabled = false;
  isActive = true;
  textColor = 'blue';
}
```

### 3. Event Binding (One-way: View → Component)

```typescript
@Component({
  template: `
    <button (click)="onClick()">Click</button>
    <input (input)="onInput($event)" />
    <input (keyup.enter)="onEnter()" />
  `
})
export class AppComponent {
  onClick() {
    console.log('Button clicked');
  }
  
  onInput(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    console.log(value);
  }
  
  onEnter() {
    console.log('Enter pressed');
  }
}
```

### 4. Two-way Binding (Component ↔ View)

```typescript
import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <input [(ngModel)]="name" />
    <p>Hello, {{ name }}!</p>
  `
})
export class AppComponent {
  name = 'Angular';
}
```

## Built-in Directives

### Structural Directives

#### *ngIf

```typescript
@Component({
  template: `
    <div *ngIf="isLoggedIn">Welcome back!</div>
    
    <div *ngIf="user; else loading">
      {{ user.name }}
    </div>
    <ng-template #loading>
      <p>Loading...</p>
    </ng-template>
    
    <!-- Angular 17+ @if syntax -->
    @if (isLoggedIn) {
      <div>Welcome back!</div>
    } @else {
      <div>Please login</div>
    }
  `
})
export class AppComponent {
  isLoggedIn = true;
  user = { name: 'John' };
}
```

#### *ngFor

```typescript
@Component({
  template: `
    <ul>
      <li *ngFor="let item of items">{{ item }}</li>
    </ul>
    
    <ul>
      <li *ngFor="let user of users; let i = index; trackBy: trackByUserId">
        {{ i + 1 }}. {{ user.name }}
      </li>
    </ul>
    
    <!-- Angular 17+ @for syntax -->
    @for (user of users; track user.id) {
      <li>{{ user.name }}</li>
    }
  `
})
export class AppComponent {
  items = ['Item 1', 'Item 2', 'Item 3'];
  users = [
    { id: 1, name: 'John' },
    { id: 2, name: 'Jane' }
  ];
  
  trackByUserId(index: number, user: any) {
    return user.id;
  }
}
```

#### *ngSwitch

```typescript
@Component({
  template: `
    <div [ngSwitch]="role">
      <p *ngSwitchCase="'admin'">Admin Panel</p>
      <p *ngSwitchCase="'user'">User Dashboard</p>
      <p *ngSwitchDefault>Guest View</p>
    </div>
    
    <!-- Angular 17+ @switch syntax -->
    @switch (role) {
      @case ('admin') {
        <p>Admin Panel</p>
      }
      @case ('user') {
        <p>User Dashboard</p>
      }
      @default {
        <p>Guest View</p>
      }
    }
  `
})
export class AppComponent {
  role = 'admin';
}
```

### Attribute Directives

#### ngClass

```typescript
@Component({
  template: `
    <div [ngClass]="'active'">Single class</div>
    <div [ngClass]="['active', 'highlight']">Multiple classes</div>
    <div [ngClass]="{ active: isActive, disabled: isDisabled }">Conditional</div>
  `
})
export class AppComponent {
  isActive = true;
  isDisabled = false;
}
```

#### ngStyle

```typescript
@Component({
  template: `
    <div [ngStyle]="{ color: 'red', 'font-size': '20px' }">Styled</div>
    <div [ngStyle]="getStyles()">Dynamic styles</div>
  `
})
export class AppComponent {
  getStyles() {
    return {
      color: this.isActive ? 'green' : 'red',
      'font-weight': 'bold'
    };
  }
  
  isActive = true;
}
```

## Custom Directives

### Attribute Directive

```typescript
import { Directive, ElementRef, HostListener, Input } from '@angular/core';

@Directive({
  selector: '[appHighlight]',
  standalone: true
})
export class HighlightDirective {
  @Input() appHighlight = 'yellow';
  
  constructor(private el: ElementRef) {}
  
  @HostListener('mouseenter') onMouseEnter() {
    this.highlight(this.appHighlight);
  }
  
  @HostListener('mouseleave') onMouseLeave() {
    this.highlight('');
  }
  
  private highlight(color: string) {
    this.el.nativeElement.style.backgroundColor = color;
  }
}

// Usage
@Component({
  template: `<p appHighlight="lightblue">Hover me!</p>`
})
```

### Structural Directive

```typescript
import { Directive, Input, TemplateRef, ViewContainerRef } from '@angular/core';

@Directive({
  selector: '[appUnless]',
  standalone: true
})
export class UnlessDirective {
  @Input() set appUnless(condition: boolean) {
    if (!condition) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    } else {
      this.viewContainer.clear();
    }
  }
  
  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef
  ) {}
}

// Usage
@Component({
  template: `<p *appUnless="isHidden">Visible when isHidden is false</p>`
})
export class AppComponent {
  isHidden = false;
}
```

## Template Reference Variables

```typescript
@Component({
  template: `
    <input #myInput type="text" />
    <button (click)="logValue(myInput.value)">Log</button>
    
    <app-child #childComponent></app-child>
    <button (click)="childComponent.someMethod()">Call Child Method</button>
  `
})
export class AppComponent {
  logValue(value: string) {
    console.log(value);
  }
}
```

## Safe Navigation Operator

```typescript
@Component({
  template: `
    <!-- Without safe navigation - throws error if user is null -->
    <p>{{ user.name }}</p>
    
    <!-- With safe navigation - returns null if user is null -->
    <p>{{ user?.name }}</p>
    
    <!-- Chaining -->
    <p>{{ user?.address?.city }}</p>
  `
})
export class AppComponent {
  user: any = null;
}
```

## Interview Questions

**Q: What are the types of data binding in Angular?**
1. Interpolation: {{ }}
2. Property binding: [property]
3. Event binding: (event)
4. Two-way binding: [(ngModel)]

**Q: What's the difference between structural and attribute directives?**
- Structural: Change DOM structure (*ngIf, *ngFor, *ngSwitch)
- Attribute: Change appearance/behavior (ngClass, ngStyle)

**Q: What is the purpose of trackBy in *ngFor?**
- Improves performance by tracking items by unique identifier
- Prevents unnecessary DOM re-creation
- Angular can reuse existing DOM elements

**Q: How to create a custom directive?**
- Use @Directive decorator
- Structural: Use TemplateRef and ViewContainerRef
- Attribute: Use ElementRef and HostListener

**Q: What is the safe navigation operator?**
- Operator: ?
- Prevents errors when accessing properties of null/undefined
- Returns null instead of throwing error

**Q: What's the difference between ngIf and hidden?**
- ngIf: Removes/adds element from DOM
- hidden: Element stays in DOM, just hidden
- ngIf better for performance with large components

**Q: What are Angular 17+ control flow improvements?**
- New syntax: @if, @for, @switch
- Better performance
- More readable
- Built-in, no imports needed
