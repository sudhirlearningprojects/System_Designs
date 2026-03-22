# Routing and Navigation

## Basic Routing Setup

### App Routes Configuration

```typescript
import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { AboutComponent } from './about/about.component';
import { NotFoundComponent } from './not-found/not-found.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'about', component: AboutComponent },
  { path: '**', component: NotFoundComponent }  // Wildcard route
];
```

### Module-based Setup

```typescript
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'about', component: AboutComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
```

### Standalone Setup

```typescript
// main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';

bootstrapApplication(AppComponent, {
  providers: [provideRouter(routes)]
});
```

### Router Outlet

```typescript
@Component({
  selector: 'app-root',
  template: `
    <nav>
      <a routerLink="/">Home</a>
      <a routerLink="/about">About</a>
    </nav>
    <router-outlet></router-outlet>
  `
})
export class AppComponent {}
```

## Navigation

### RouterLink

```typescript
@Component({
  template: `
    <!-- Basic -->
    <a routerLink="/about">About</a>
    
    <!-- With parameters -->
    <a [routerLink]="['/user', userId]">User Profile</a>
    
    <!-- With query params -->
    <a [routerLink]="['/search']" [queryParams]="{q: 'angular'}">Search</a>
    
    <!-- Active link styling -->
    <a routerLink="/about" routerLinkActive="active">About</a>
    
    <!-- Exact match -->
    <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">
      Home
    </a>
  `
})
```

### Programmatic Navigation

```typescript
import { Component } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-user'
})
export class UserComponent {
  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) {}
  
  navigateToAbout() {
    this.router.navigate(['/about']);
  }
  
  navigateWithParams() {
    this.router.navigate(['/user', 123]);
  }
  
  navigateWithQueryParams() {
    this.router.navigate(['/search'], {
      queryParams: { q: 'angular', page: 1 }
    });
  }
  
  navigateRelative() {
    // Relative to current route
    this.router.navigate(['../sibling'], { relativeTo: this.route });
  }
  
  goBack() {
    window.history.back();
  }
}
```

## Route Parameters

### Path Parameters

```typescript
// Route configuration
const routes: Routes = [
  { path: 'user/:id', component: UserDetailComponent }
];

// Component
@Component({
  selector: 'app-user-detail'
})
export class UserDetailComponent implements OnInit {
  userId: string = '';
  
  constructor(private route: ActivatedRoute) {}
  
  ngOnInit() {
    // Snapshot (one-time read)
    this.userId = this.route.snapshot.paramMap.get('id') || '';
    
    // Observable (reactive)
    this.route.paramMap.subscribe(params => {
      this.userId = params.get('id') || '';
      this.loadUser(this.userId);
    });
  }
  
  loadUser(id: string) {
    // Load user data
  }
}
```

### Query Parameters

```typescript
// Navigate with query params
this.router.navigate(['/search'], {
  queryParams: { q: 'angular', page: 1 }
});

// Read query params
@Component({
  selector: 'app-search'
})
export class SearchComponent implements OnInit {
  constructor(private route: ActivatedRoute) {}
  
  ngOnInit() {
    // Snapshot
    const query = this.route.snapshot.queryParamMap.get('q');
    
    // Observable
    this.route.queryParamMap.subscribe(params => {
      const query = params.get('q');
      const page = params.get('page');
      this.search(query, page);
    });
  }
}
```

## Nested Routes

```typescript
const routes: Routes = [
  {
    path: 'dashboard',
    component: DashboardComponent,
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview', component: OverviewComponent },
      { path: 'stats', component: StatsComponent },
      { path: 'settings', component: SettingsComponent }
    ]
  }
];

// Dashboard component
@Component({
  selector: 'app-dashboard',
  template: `
    <nav>
      <a routerLink="overview">Overview</a>
      <a routerLink="stats">Stats</a>
      <a routerLink="settings">Settings</a>
    </nav>
    <router-outlet></router-outlet>
  `
})
export class DashboardComponent {}
```

## Lazy Loading

```typescript
const routes: Routes = [
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule)
  },
  {
    path: 'user',
    loadComponent: () => import('./user/user.component').then(m => m.UserComponent)
  }
];

// Admin module routes
const adminRoutes: Routes = [
  { path: '', component: AdminDashboardComponent },
  { path: 'users', component: AdminUsersComponent }
];

@NgModule({
  imports: [RouterModule.forChild(adminRoutes)]
})
export class AdminModule {}
```

## Route Guards

### CanActivate (Protect routes)

```typescript
import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private router: Router) {}
  
  canActivate(): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    const isLoggedIn = !!localStorage.getItem('token');
    
    if (isLoggedIn) {
      return true;
    }
    
    return this.router.createUrlTree(['/login']);
  }
}

// Route configuration
const routes: Routes = [
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  }
];
```

### Functional Guards (Angular 15+)

```typescript
import { inject } from '@angular/core';
import { Router } from '@angular/router';

export const authGuard = () => {
  const router = inject(Router);
  const isLoggedIn = !!localStorage.getItem('token');
  
  return isLoggedIn ? true : router.createUrlTree(['/login']);
};

// Usage
const routes: Routes = [
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  }
];
```

### CanDeactivate (Prevent navigation)

```typescript
import { Injectable } from '@angular/core';
import { CanDeactivate } from '@angular/router';

export interface CanComponentDeactivate {
  canDeactivate: () => boolean | Observable<boolean>;
}

@Injectable({
  providedIn: 'root'
})
export class UnsavedChangesGuard implements CanDeactivate<CanComponentDeactivate> {
  canDeactivate(component: CanComponentDeactivate) {
    return component.canDeactivate ? component.canDeactivate() : true;
  }
}

// Component
@Component({
  selector: 'app-form'
})
export class FormComponent implements CanComponentDeactivate {
  hasUnsavedChanges = false;
  
  canDeactivate() {
    if (this.hasUnsavedChanges) {
      return confirm('You have unsaved changes. Do you want to leave?');
    }
    return true;
  }
}

// Route
const routes: Routes = [
  {
    path: 'form',
    component: FormComponent,
    canDeactivate: [UnsavedChangesGuard]
  }
];
```

### Resolve (Pre-fetch data)

```typescript
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserResolver implements Resolve<any> {
  constructor(private userService: UserService) {}
  
  resolve(route: ActivatedRouteSnapshot): Observable<any> {
    const id = route.paramMap.get('id');
    return this.userService.getUserById(id);
  }
}

// Route
const routes: Routes = [
  {
    path: 'user/:id',
    component: UserDetailComponent,
    resolve: { user: UserResolver }
  }
];

// Component
@Component({
  selector: 'app-user-detail'
})
export class UserDetailComponent implements OnInit {
  user: any;
  
  constructor(private route: ActivatedRoute) {}
  
  ngOnInit() {
    this.user = this.route.snapshot.data['user'];
  }
}
```

## Route Configuration Options

```typescript
const routes: Routes = [
  {
    path: 'products',
    component: ProductsComponent,
    data: { title: 'Products', breadcrumb: 'Products' },  // Static data
    canActivate: [AuthGuard],
    canDeactivate: [UnsavedChangesGuard],
    resolve: { products: ProductsResolver },
    children: [
      { path: ':id', component: ProductDetailComponent }
    ]
  },
  {
    path: 'old-path',
    redirectTo: '/new-path',
    pathMatch: 'full'
  }
];
```

## Interview Questions

**Q: What is routing in Angular?**
- Mechanism for navigation between views
- Maps URLs to components
- Supports lazy loading and guards
- Provides navigation history

**Q: What's the difference between routerLink and href?**
- routerLink: Angular navigation, no page reload
- href: Traditional navigation, full page reload
- routerLink maintains app state

**Q: What are route guards?**
- Interfaces to control navigation
- CanActivate: Protect routes
- CanDeactivate: Prevent leaving
- Resolve: Pre-fetch data
- CanLoad: Lazy loading control

**Q: What is lazy loading?**
- Load modules on demand
- Reduces initial bundle size
- Improves performance
- Uses loadChildren or loadComponent

**Q: Snapshot vs Observable for route params?**
- Snapshot: One-time read, component recreated
- Observable: Reactive, component reused
- Use Observable when navigating to same route with different params

**Q: What is pathMatch: 'full'?**
- Exact URL match required
- Used with redirects and empty paths
- Without it, partial matches allowed

**Q: How to pass data between routes?**
- Route parameters: /user/:id
- Query parameters: /search?q=angular
- State: router.navigate with state object
- Service: Shared service with BehaviorSubject
