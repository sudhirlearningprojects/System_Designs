# Services and Dependency Injection

## Services

Services are classes that handle business logic, data access, and shared functionality.

### Creating a Service

```typescript
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'  // Singleton service
})
export class UserService {
  private users = [
    { id: 1, name: 'John' },
    { id: 2, name: 'Jane' }
  ];
  
  getUsers() {
    return this.users;
  }
  
  getUserById(id: number) {
    return this.users.find(user => user.id === id);
  }
  
  addUser(user: any) {
    this.users.push(user);
  }
}
```

### Using a Service

```typescript
import { Component, OnInit } from '@angular/core';
import { UserService } from './user.service';

@Component({
  selector: 'app-user-list',
  template: `
    <ul>
      <li *ngFor="let user of users">{{ user.name }}</li>
    </ul>
  `
})
export class UserListComponent implements OnInit {
  users: any[] = [];
  
  constructor(private userService: UserService) {}
  
  ngOnInit() {
    this.users = this.userService.getUsers();
  }
}
```

## Dependency Injection

### Provider Scope

#### 1. Root Level (Singleton)

```typescript
@Injectable({
  providedIn: 'root'
})
export class DataService {
  // Single instance across entire app
}
```

#### 2. Module Level

```typescript
@NgModule({
  providers: [DataService]  // One instance per module
})
export class FeatureModule {}
```

#### 3. Component Level

```typescript
@Component({
  selector: 'app-user',
  providers: [DataService]  // New instance per component
})
export class UserComponent {}
```

### Injection Tokens

```typescript
import { InjectionToken } from '@angular/core';

export const API_URL = new InjectionToken<string>('api.url');

// Provide
@NgModule({
  providers: [
    { provide: API_URL, useValue: 'https://api.example.com' }
  ]
})

// Inject
constructor(@Inject(API_URL) private apiUrl: string) {
  console.log(this.apiUrl);
}
```

### Provider Types

#### useClass

```typescript
@NgModule({
  providers: [
    { provide: LoggerService, useClass: ConsoleLoggerService }
  ]
})
```

#### useValue

```typescript
const config = { apiUrl: 'https://api.example.com' };

@NgModule({
  providers: [
    { provide: 'APP_CONFIG', useValue: config }
  ]
})
```

#### useFactory

```typescript
export function loggerFactory(isDev: boolean) {
  return isDev ? new ConsoleLogger() : new FileLogger();
}

@NgModule({
  providers: [
    {
      provide: LoggerService,
      useFactory: loggerFactory,
      deps: [IS_DEV_MODE]
    }
  ]
})
```

#### useExisting

```typescript
@NgModule({
  providers: [
    LoggerService,
    { provide: 'AliasLogger', useExisting: LoggerService }
  ]
})
```

## HTTP Client

### Setup

```typescript
import { HttpClientModule } from '@angular/common/http';

@NgModule({
  imports: [HttpClientModule]
})
export class AppModule {}

// Standalone
import { provideHttpClient } from '@angular/common/http';

bootstrapApplication(AppComponent, {
  providers: [provideHttpClient()]
});
```

### Basic HTTP Service

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = 'https://api.example.com';
  
  constructor(private http: HttpClient) {}
  
  // GET
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }
  
  // GET with params
  getUserById(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/users/${id}`);
  }
  
  // POST
  createUser(user: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/users`, user);
  }
  
  // PUT
  updateUser(id: number, user: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${id}`, user);
  }
  
  // DELETE
  deleteUser(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/users/${id}`);
  }
  
  // With headers
  getUsersWithAuth(): Observable<any[]> {
    const headers = new HttpHeaders({
      'Authorization': 'Bearer token123'
    });
    return this.http.get<any[]>(`${this.apiUrl}/users`, { headers });
  }
  
  // With query params
  searchUsers(query: string): Observable<any[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<any[]>(`${this.apiUrl}/users`, { params });
  }
}
```

### Using HTTP Service

```typescript
@Component({
  selector: 'app-users',
  template: `
    <div *ngIf="loading">Loading...</div>
    <div *ngIf="error">{{ error }}</div>
    <ul *ngIf="users">
      <li *ngFor="let user of users">{{ user.name }}</li>
    </ul>
  `
})
export class UsersComponent implements OnInit {
  users: any[] = [];
  loading = false;
  error: string | null = null;
  
  constructor(private apiService: ApiService) {}
  
  ngOnInit() {
    this.loading = true;
    this.apiService.getUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }
}
```

## HTTP Interceptors

### Creating an Interceptor

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('token');
    
    if (token) {
      const cloned = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });
      return next.handle(cloned);
    }
    
    return next.handle(req);
  }
}
```

### Error Handling Interceptor

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler) {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Redirect to login
        }
        return throwError(() => error);
      })
    );
  }
}
```

### Registering Interceptors

```typescript
import { HTTP_INTERCEPTORS } from '@angular/common/http';

@NgModule({
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }
  ]
})
export class AppModule {}

// Standalone
import { provideHttpClient, withInterceptors } from '@angular/common/http';

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    )
  ]
});
```

## RxJS with Services

### Using Operators

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, catchError, tap, shareReplay } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private usersSubject = new BehaviorSubject<any[]>([]);
  users$ = this.usersSubject.asObservable();
  
  constructor(private http: HttpClient) {}
  
  loadUsers() {
    this.http.get<any[]>('/api/users').pipe(
      tap(users => console.log('Users loaded:', users)),
      catchError(error => {
        console.error('Error loading users:', error);
        return [];
      })
    ).subscribe(users => this.usersSubject.next(users));
  }
  
  getUserNames(): Observable<string[]> {
    return this.users$.pipe(
      map(users => users.map(user => user.name))
    );
  }
  
  // Cache HTTP response
  private cache$?: Observable<any[]>;
  
  getUsersWithCache(): Observable<any[]> {
    if (!this.cache$) {
      this.cache$ = this.http.get<any[]>('/api/users').pipe(
        shareReplay(1)
      );
    }
    return this.cache$;
  }
}
```

## Interview Questions

**Q: What is a service in Angular?**
- Class that handles business logic and data
- Decorated with @Injectable
- Injected into components via DI
- Promotes code reusability

**Q: What is Dependency Injection?**
- Design pattern for managing dependencies
- Angular's DI system provides instances
- Configured via providers
- Supports different scopes (root, module, component)

**Q: What does providedIn: 'root' mean?**
- Service is singleton across entire app
- Lazy loaded (tree-shakeable)
- No need to add to providers array
- Recommended approach

**Q: What are HTTP interceptors?**
- Middleware for HTTP requests/responses
- Can modify requests (add headers, auth tokens)
- Handle errors globally
- Multiple interceptors can be chained

**Q: How to handle HTTP errors?**
- Use catchError operator
- Implement error interceptor
- Show user-friendly messages
- Log errors for debugging

**Q: What's the difference between Subject and BehaviorSubject?**
- Subject: No initial value, only emits new values
- BehaviorSubject: Has initial value, emits last value to new subscribers

**Q: What is the purpose of shareReplay?**
- Caches HTTP response
- Shares single subscription among multiple subscribers
- Prevents duplicate HTTP calls
- Useful for expensive operations
