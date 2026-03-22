# Components and Modules

## Components

Components are the building blocks of Angular applications. Each component controls a portion of the screen called a view.

### Creating a Component

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-hello',
  template: `<h1>Hello, {{ name }}!</h1>`,
  styles: [`h1 { color: blue; }`]
})
export class HelloComponent {
  name = 'Angular';
}
```

### Component Decorator Properties

```typescript
@Component({
  selector: 'app-user',           // CSS selector
  templateUrl: './user.component.html',  // External template
  styleUrls: ['./user.component.css'],   // External styles
  standalone: true,                // Standalone component (Angular 14+)
  imports: [CommonModule],         // For standalone components
  providers: [UserService]         // Component-level services
})
```

### Component Lifecycle Hooks

```typescript
import { Component, OnInit, OnDestroy, OnChanges, SimpleChanges } from '@angular/core';

export class UserComponent implements OnInit, OnDestroy, OnChanges {
  
  // Called once after first ngOnChanges
  ngOnInit(): void {
    console.log('Component initialized');
  }
  
  // Called when input properties change
  ngOnChanges(changes: SimpleChanges): void {
    console.log('Input changed:', changes);
  }
  
  // Called after view initialization
  ngAfterViewInit(): void {
    console.log('View initialized');
  }
  
  // Called before component destruction
  ngOnDestroy(): void {
    console.log('Component destroyed');
  }
}
```

### Complete Lifecycle Order

1. **ngOnChanges** - When input properties change
2. **ngOnInit** - After first ngOnChanges
3. **ngDoCheck** - During every change detection
4. **ngAfterContentInit** - After content projection
5. **ngAfterContentChecked** - After content checked
6. **ngAfterViewInit** - After view initialization
7. **ngAfterViewChecked** - After view checked
8. **ngOnDestroy** - Before component destruction

## Modules (NgModule)

Modules organize an application into cohesive blocks of functionality.

### Basic Module

```typescript
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { UserComponent } from './user/user.component';

@NgModule({
  declarations: [    // Components, directives, pipes
    AppComponent,
    UserComponent
  ],
  imports: [         // Other modules
    BrowserModule,
    FormsModule
  ],
  providers: [],     // Services
  bootstrap: [AppComponent]  // Root component
})
export class AppModule { }
```

### Feature Module

```typescript
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { UserListComponent } from './user-list/user-list.component';
import { UserDetailComponent } from './user-detail/user-detail.component';

@NgModule({
  declarations: [
    UserListComponent,
    UserDetailComponent
  ],
  imports: [
    CommonModule,
    RouterModule.forChild([
      { path: '', component: UserListComponent },
      { path: ':id', component: UserDetailComponent }
    ])
  ],
  exports: [UserListComponent]  // Make available to other modules
})
export class UserModule { }
```

## Standalone Components (Angular 14+)

Standalone components don't need NgModule.

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-user',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <h2>{{ user.name }}</h2>
    <input [(ngModel)]="user.email" />
  `
})
export class UserComponent {
  user = { name: 'John', email: 'john@example.com' };
}
```

### Standalone App Bootstrap

```typescript
// main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, {
  providers: [
    // Global providers
  ]
});
```

## Component Communication

### Parent to Child (@Input)

```typescript
// Parent
@Component({
  template: `<app-child [message]="parentMessage"></app-child>`
})
export class ParentComponent {
  parentMessage = 'Hello from parent';
}

// Child
@Component({
  selector: 'app-child'
})
export class ChildComponent {
  @Input() message: string = '';
}
```

### Child to Parent (@Output)

```typescript
// Child
import { Component, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-child',
  template: `<button (click)="sendMessage()">Send</button>`
})
export class ChildComponent {
  @Output() messageEvent = new EventEmitter<string>();
  
  sendMessage() {
    this.messageEvent.emit('Hello from child');
  }
}

// Parent
@Component({
  template: `<app-child (messageEvent)="receiveMessage($event)"></app-child>`
})
export class ParentComponent {
  receiveMessage(message: string) {
    console.log(message);
  }
}
```

### ViewChild and ContentChild

```typescript
import { Component, ViewChild, ContentChild, ElementRef } from '@angular/core';

@Component({
  selector: 'app-parent',
  template: `
    <input #myInput />
    <app-child>
      <p #projectedContent>Projected content</p>
    </app-child>
  `
})
export class ParentComponent {
  @ViewChild('myInput') input!: ElementRef;
  @ViewChild(ChildComponent) child!: ChildComponent;
  
  ngAfterViewInit() {
    console.log(this.input.nativeElement.value);
    this.child.someMethod();
  }
}

@Component({
  selector: 'app-child',
  template: `<ng-content></ng-content>`
})
export class ChildComponent {
  @ContentChild('projectedContent') content!: ElementRef;
  
  ngAfterContentInit() {
    console.log(this.content.nativeElement.textContent);
  }
}
```

## Interview Questions

**Q: What is a component in Angular?**
- Building block of Angular applications
- Controls a view (portion of screen)
- Consists of TypeScript class, HTML template, CSS styles
- Decorated with @Component

**Q: What are lifecycle hooks?**
- Methods called at specific points in component lifecycle
- Used for initialization, cleanup, change detection
- Common: ngOnInit, ngOnDestroy, ngOnChanges

**Q: What's the difference between constructor and ngOnInit?**
- Constructor: TypeScript class initialization, dependency injection
- ngOnInit: Angular-specific initialization, after input properties set

**Q: What are standalone components?**
- Angular 14+ feature
- Don't require NgModule
- Import dependencies directly
- Simplify application structure

**Q: @Input vs @Output?**
- @Input: Pass data from parent to child
- @Output: Emit events from child to parent
- @Output uses EventEmitter

**Q: ViewChild vs ContentChild?**
- ViewChild: Query elements in component's template
- ContentChild: Query projected content (ng-content)
- Both available after view/content initialization

**Q: What is change detection?**
- Mechanism to sync component state with view
- Runs when events occur (click, HTTP, timers)
- Can be optimized with OnPush strategy
