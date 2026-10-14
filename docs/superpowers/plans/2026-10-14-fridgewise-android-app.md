# FridgeWise Android App — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Jetpack Compose + MVVM Android app for FridgeWise — a food tracking, recipe discovery, and household fridge management application with 9 core screens, bottom navigation, scan wizard, and API integration.

**Architecture:** Single-activity Compose app with `Scaffold` + bottom `NavigationBar` (5 tabs). Each feature module = 1 Screen + 1 ViewModel. Data flows through Repository → Retrofit API → ViewModel → `StateFlow` → `collectAsStateWithLifecycle()`. Navigation via Compose Navigation with type-safe routes.

**Tech Stack:** Kotlin 2.2, Compose BOM 2026.06, Material3, Compose Navigation, Retrofit2 + OkHttp + Moshi, Hilt (DI), Coil (images), ExoPlayer (reels video), Supabase Auth (login).

**Spec:** `FridgeWise_Functional_Specification.md` (flow, §12 invariants), `API.md` (endpoints), `AGENTS.md` (Compose rules)

## Global Constraints

- `minSdk 23`, `compileSdk 36`, theme `MyAppTheme`
- All colors from `MaterialTheme.colorScheme`; all text from `MaterialTheme.typography` — no hardcoded `Color(...)` or `fontSize` outside `ui/theme/`
- Font: Plus Jakarta Sans → mapped to `MaterialTheme.typography` (not bundled yet)
- Every public composable has `@Preview` wrapped in `MyAppTheme`
- `Scaffold` + `innerPadding` on all screens (`enableEdgeToEdge()`)
- Modifier order: layout → padding → interaction/appearance
- State hoisting: leaf composables stateless `(value, onEvent)`; business logic in ViewModel
- `LazyColumn`/`LazyRow` always with `key`
- Bottom nav order (§12): Reels → Favorite → Scan → Family → Inventory
- Default landing screen: Inventory (§12)
- File limit: ~300 lines per file; split if exceeding
- `rememberSaveable` for dialog/sheet open state
- Only Material3 `AlertDialog` and `ModalBottomSheet` — no raw `Dialog`/`PopupWindow`

## Review Focus

1. **Offline / no-network state**: API calls fail silently — screens must show loading/error states, not crash or hang.
2. **Empty fridge (zero foods)**: Inventory, StorageList, RecipeMatch all must handle empty data gracefully with empty-state UI.
3. **Scan wizard back-press during each step**: User presses back mid-scan — must not commit partial data; each step must be cancellable without corrupting state.
4. **Expired JWT / 401 during active session**: Token expires while app is in use — need global 401 interceptor that redirects to login.
5. **Deep-link from Reel to Recipe Detail with no fridge_id**: `haveNeed` field requires `fridge_id` — must handle nullable gracefully in Recipe Detail.

---

## File Structure

```
app/src/main/kotlin/com/production/fridgewise/
├── MainActivity.kt                          # Single activity, edge-to-edge, theme, nav host
├── FridgeWiseApp.kt                         # Top-level Composable: Scaffold + BottomNav + NavHost
├── navigation/
│   ├── Route.kt                             # Sealed class / enum for all routes
│   ├── NavGraph.kt                          # NavHost with all composable destinations
│   └── BottomNavBar.kt                      # 5-tab bottom navigation bar
├── data/
│   ├── remote/
│   │   ├── ApiService.kt                    # Retrofit interface (all endpoints)
│   │   ├── AuthInterceptor.kt               # OkHttp interceptor: Bearer token injection
│   │   └── dto/                             # Request/Response data classes
│   │       ├── ScanDto.kt                   # CreateScan, UpdateItems, UpdateLocations, CommitScan
│   │       ├── FridgeDto.kt                 # Fridge, FoodItem, FridgeStats, Member, JoinRequest
│   │       ├── RecipeDto.kt                 # RecipeCard, RecipeDetail, HaveNeed, SearchParams
│   │       └── FavoriteDto.kt               # LikeRequest, LikeResponse, UnlikeResponse
│   ├── repository/
│   │   ├── AuthRepository.kt                # Supabase auth (login, logout, token)
│   │   ├── ScanRepository.kt                # POST/PUT scans, commit
│   │   ├── FridgeRepository.kt              # Fridge CRUD, foods, stats, members
│   │   ├── RecipeRepository.kt              # Match, search, detail, reels
│   │   └── FavoriteRepository.kt            # Like/unlike, list favorites
│   └── di/
│       └── NetworkModule.kt                 # Hilt module: Retrofit, OkHttp, ApiService
├── ui/
│   ├── theme/
│   │   ├── Color.kt                         # (existing — update with FridgeWise palette)
│   │   ├── Theme.kt                         # (existing — keep MyAppTheme)
│   │   └── Type.kt                          # (existing — update with Plus Jakarta Sans scales)
│   ├── components/
│   │   ├── ExpiryBadge.kt                   # Red/Yellow/Gray badge (§12 invariant)
│   │   ├── FoodItemCard.kt                  # Reusable food item row (icon, name, expiry, badge)
│   │   ├── RecipeCard.kt                    # Reusable recipe card (image, title, meta)
│   │   ├── LoadingState.kt                  # Full-screen / inline loading indicator
│   │   ├── ErrorState.kt                    # Full-screen / inline error with retry
│   │   └── EmptyState.kt                    # Empty list placeholder
│   └── screens/
│       ├── login/
│       │   ├── LoginScreen.kt               # Google, Facebook, Email/Password login
│       │   └── LoginViewModel.kt
│       ├── setup/
│       │   ├── SetupFridgeScreen.kt          # Enter fridge name
│       │   └── SetupFridgeViewModel.kt
│       ├── inventory/
│       │   ├── InventoryScreen.kt            # Dashboard: stats, freezer/fridge sections, "Nấu ăn"
│       │   └── InventoryViewModel.kt
│       ├── storagelist/
│       │   ├── StorageListScreen.kt          # Filtered food list by location
│       │   └── StorageListViewModel.kt
│       ├── scan/
│       │   ├── ScanScreen.kt                 # Camera / photo picker → OCR
│       │   ├── ConfirmItemsScreen.kt         # Review OCR results, edit, add manual
│       │   ├── AssignStorageScreen.kt         # Drag-and-drop to freezer/fridge, "Hoàn tất"
│       │   └── ScanViewModel.kt              # Shared across 3 scan steps (wizard state machine)
│       ├── recipes/
│       │   ├── RecipeListScreen.kt           # Matched recipes with search/filter
│       │   ├── RecipeListViewModel.kt
│       │   ├── RecipeDetailScreen.kt         # Full recipe + Have/Need bottom sheets
│       │   └── RecipeDetailViewModel.kt
│       ├── reels/
│       │   ├── ReelsScreen.kt                # Full-screen video feed (ExoPlayer)
│       │   └── ReelsViewModel.kt
│       ├── favorite/
│       │   ├── FavoriteScreen.kt             # Liked/saved recipes grid
│       │   └── FavoriteViewModel.kt
│       ├── family/
│       │   ├── FamilyScreen.kt               # Members list, invite code, join
│       │   └── FamilyViewModel.kt
│       └── settings/
│           ├── SettingsScreen.kt             # Account, fridge rename, preferences
│           └── SettingsViewModel.kt
```

---

## Task 1: Gradle Dependencies & DI Setup

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/kotlin/com/production/fridgewise/FridgeWiseApplication.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/di/NetworkModule.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: nothing
- Produces: `ApiService` (Retrofit interface injectable via Hilt), `OkHttpClient` with `AuthInterceptor`, `@HiltAndroidApp` application class

- [ ] **Step 1: Add dependency versions to `libs.versions.toml`**

Add versions and libraries for: `navigation-compose`, `hilt-android`, `hilt-navigation-compose`, `retrofit2`, `converter-moshi`, `moshi-kotlin`, `okhttp-logging-interceptor`, `coil-compose`, `media3-exoplayer` + `media3-ui`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `kotlinx-serialization`. Add KSP and Hilt plugins.

- [ ] **Step 2: Apply plugins in root and app `build.gradle.kts`**

Root: add `kotlin-kapt` / `ksp` and `hilt-android` plugin (apply false). App: apply `kotlin-kapt`/`ksp`, `hilt-android`, add all `implementation()` lines.

- [ ] **Step 3: Create `FridgeWiseApplication.kt`**

```kotlin
@HiltAndroidApp
class FridgeWiseApplication : Application()
```

- [ ] **Step 4: Create `NetworkModule.kt`**

Hilt `@Module` `@InstallIn(SingletonComponent)` providing:
- `@Provides @Singleton` `OkHttpClient` with `AuthInterceptor` + `HttpLoggingInterceptor`
- `@Provides @Singleton` `Retrofit` (base URL from BuildConfig, Moshi converter)
- `@Provides @Singleton` `ApiService` (Retrofit.create)

- [ ] **Step 5: Create `AuthInterceptor.kt`**

OkHttp `Interceptor` that reads stored JWT and adds `Authorization: Bearer <token>` header. On 401 response, posts event to redirect to login.

- [ ] **Step 6: Update `AndroidManifest.xml`**

Add `android:name=".FridgeWiseApplication"` to `<application>`, add `<uses-permission android:name="android.permission.INTERNET"/>`, add `<uses-permission android:name="android.permission.CAMERA"/>`.

- [ ] **Step 7: Build and verify compilation**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "chore: add Hilt DI, Retrofit, Coil, Navigation, ExoPlayer dependencies"
```

---

## Task 2: API Service & DTOs

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/data/remote/ApiService.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/remote/dto/ScanDto.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/remote/dto/FridgeDto.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/remote/dto/RecipeDto.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/remote/dto/FavoriteDto.kt`

**Interfaces:**
- Consumes: `Retrofit` from Task 1
- Produces: `ApiService` interface with all endpoint methods; all DTO data classes used by repositories

- [ ] **Step 1: Create `FridgeDto.kt`**

Data classes: `FridgeResponse(id, name, invite_code, role)`, `FoodItemResponse(id, name, ingredient_id, qty_display, unit_display, qty_base, base_unit, purchased_at, expiry_date, location)`, `FridgeStatsResponse(inStock, useToday, useSoon, cleanOut)`, `MemberResponse(user_id, role, name, email)`, `JoinRequest(code)`, `JoinResponse(fridgeId, already)`, `RenameFridgeRequest(name)`, `UpdateFoodRequest(name?, qty_display?, unit_display?, expiry_date?, location?)`.

- [ ] **Step 2: Create `ScanDto.kt`**

Data classes: `ScanItemRequest(name, ingredient_id, qty_display, unit_display, purchased_at, expiry_date)`, `CreateScanRequest(fridge_id, items: List<ScanItemRequest>)`, `CreateScanResponse(scanId, inserted, items: List<ScanItemId>)`, `ScanItemId(id, name)`, `UpdateScanItemRequest(id, qty_display, unit_display, purchased_at, expiry_date)`, `BulkUpdateRequest(items: List<UpdateScanItemRequest>)`, `LocationAssignment(id, location)`, `LocationsRequest(items: List<LocationAssignment>)`, `CommitResponse(inserted, already)`.

- [ ] **Step 3: Create `RecipeDto.kt`**

Data classes: `RecipeCardResponse(id, name, description, imageUrl, category, method, like_count, video_id, video_provider, servings, time, difficulty)`, `IngredientResponse(ingredientId, name, quantity, unit, qty_base, base_unit)`, `RecipeDetailResponse(info: RecipeCardResponse, ingredients: List<IngredientResponse>, detail: RecipeDetailInfo, haveNeed: HaveNeedResponse?)`, `RecipeDetailInfo(servings, time, difficulty, videoUrl, preparation, instructions, servingText)`, `HaveNeedResponse(matched: List<IngredientResponse>, missing: List<IngredientResponse>)`.

- [ ] **Step 4: Create `FavoriteDto.kt`**

Data classes: `LikeRequest(recipe_id)`, `LikeResponse(liked, already)`, `UnlikeResponse(unliked, removed)`.

- [ ] **Step 5: Create `ApiService.kt`**

Retrofit interface with all endpoints:
- `@GET("fridges")` → `List<FridgeResponse>`
- `@GET("fridges/{id}/foods")` with `@Query("location")`, `@Query("q")` → `List<FoodItemResponse>`
- `@GET("fridges/{id}/stats")` → `FridgeStatsResponse`
- `@PATCH("fridges/{id}")` → `UpdatedResponse`
- `@PATCH("fridges/foods/{id}")` → `UpdatedResponse`
- `@DELETE("fridges/foods/{id}")` → `DeletedResponse`
- `@GET("fridges/{id}/members")` → `List<MemberResponse>`
- `@POST("fridges/join")` → `JoinResponse`
- `@POST("scans")` → `CreateScanResponse`
- `@PUT("scans/{id}/items")` → `UpdatedResponse`
- `@PUT("scans/{id}/locations")` → `UpdatedResponse`
- `@POST("scans/{id}/commit")` → `CommitResponse`
- `@GET("recipes/match")` with `@Query` → `List<RecipeCardResponse>`
- `@GET("recipes/search")` with `@Query` → `List<RecipeCardResponse>`
- `@GET("recipes/reels")` → `List<RecipeCardResponse>`
- `@GET("recipes/{id}")` with `@Query("fridge_id")` → `RecipeDetailResponse`
- `@GET("favorites")` → `List<RecipeCardResponse>`
- `@POST("favorites")` → `LikeResponse`
- `@DELETE("favorites/{recipeId}")` → `UnlikeResponse`

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add API service interface and all DTO data classes"
```

---

## Task 3: Repositories

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/data/repository/AuthRepository.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/repository/FridgeRepository.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/repository/ScanRepository.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/repository/RecipeRepository.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/data/repository/FavoriteRepository.kt`

**Interfaces:**
- Consumes: `ApiService` from Task 2
- Produces: Repository classes wrapping API calls with `Result<T>` return types, injectable via `@Inject constructor`

- [ ] **Step 1: Create `AuthRepository.kt`**

`@Singleton` class with Supabase client. Methods: `signInWithGoogle()`, `signInWithEmail(email, password)`, `signUp(email, password)`, `signOut()`, `getSession(): Flow<AuthState>`, `getAccessToken(): String?`. Stores token for `AuthInterceptor`.

- [ ] **Step 2: Create `FridgeRepository.kt`**

Methods wrapping `ApiService` calls: `getMyFridges(): Result<List<FridgeResponse>>`, `getFoods(fridgeId, location?, query?): Result<List<FoodItemResponse>>`, `getStats(fridgeId): Result<FridgeStatsResponse>`, `renameFridge(fridgeId, name): Result<Unit>`, `updateFood(foodId, request): Result<Unit>`, `deleteFood(foodId): Result<Unit>`, `getMembers(fridgeId): Result<List<MemberResponse>>`, `joinFridge(code): Result<JoinResponse>`.

- [ ] **Step 3: Create `ScanRepository.kt`**

Methods: `createScan(fridgeId, items): Result<CreateScanResponse>`, `updateItems(scanId, items): Result<Unit>`, `updateLocations(scanId, items): Result<Unit>`, `commitScan(scanId): Result<CommitResponse>`.

- [ ] **Step 4: Create `RecipeRepository.kt`**

Methods: `getMatchedRecipes(fridgeId, limit): Result<List<RecipeCardResponse>>`, `searchRecipes(category?, method?, name?, limit?): Result<List<RecipeCardResponse>>`, `getRecipeDetail(recipeId, fridgeId?): Result<RecipeDetailResponse>`, `getReels(limit?): Result<List<RecipeCardResponse>>`.

- [ ] **Step 5: Create `FavoriteRepository.kt`**

Methods: `getFavorites(): Result<List<RecipeCardResponse>>`, `likeRecipe(recipeId): Result<LikeResponse>`, `unlikeRecipe(recipeId): Result<UnlikeResponse>`.

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add repository layer wrapping all API endpoints"
```

---

## Task 4: Navigation & App Shell

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/navigation/Route.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/navigation/BottomNavBar.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/navigation/NavGraph.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/FridgeWiseApp.kt`
- Modify: `app/src/main/kotlin/com/production/fridgewise/MainActivity.kt`

**Interfaces:**
- Consumes: nothing
- Produces: `Route` sealed class, `BottomNavBar` composable, `NavGraph` composable, `FridgeWiseApp` top-level composable with `Scaffold` + bottom nav

- [ ] **Step 1: Create `Route.kt`**

Sealed class/interface with routes:
- Bottom tabs: `Route.Reels`, `Route.Favorite`, `Route.Scan`, `Route.Family`, `Route.Inventory`
- Sub-routes: `Route.Login`, `Route.SetupFridge`, `Route.StorageList(location: String)`, `Route.RecipeList(fridgeId: String)`, `Route.RecipeDetail(recipeId: String, fridgeId: String?)`, `Route.ConfirmItems(scanId: String)`, `Route.AssignStorage(scanId: String)`, `Route.Settings`

- [ ] **Step 2: Create `BottomNavBar.kt`**

5-tab `NavigationBar` composable. Order per §12: Reels, Favorite, Scan (center FAB-style), Family, Inventory. Uses Material3 icons. `currentRoute` parameter highlights active tab. `onTabSelected: (Route) -> Unit` callback. Preview with `MyAppTheme`.

- [ ] **Step 3: Create `NavGraph.kt`**

`NavHost` composable with `composable<Route.*>` destinations. Login/SetupFridge have no bottom bar. All main tabs and sub-screens defined. Start destination depends on auth state (Login if unauthenticated, Inventory if authenticated).

- [ ] **Step 4: Create `FridgeWiseApp.kt`**

Top-level composable: observes auth state, wraps `Scaffold` + `BottomNavBar` (hidden on Login/Setup/Scan wizard). Passes `navController` to `NavGraph`. Handles `innerPadding`.

- [ ] **Step 5: Update `MainActivity.kt`**

Replace `Greeting` with `FridgeWiseApp()`. Add `@AndroidEntryPoint`.

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — app launches with empty shells, bottom nav visible

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add navigation graph, bottom nav bar, and app shell"
```

---

## Task 5: Shared UI Components

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/components/ExpiryBadge.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/components/FoodItemCard.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/components/RecipeCard.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/components/LoadingState.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/components/ErrorState.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/components/EmptyState.kt`

**Interfaces:**
- Consumes: `MaterialTheme.colorScheme`, `MaterialTheme.typography`
- Produces: `ExpiryBadge(status: ExpiryStatus, modifier)`, `FoodItemCard(food: FoodItemUi, onClick, modifier)`, `RecipeCard(recipe: RecipeCardUi, onClick, onFavoriteClick?, modifier)`, `LoadingState(modifier)`, `ErrorState(message, onRetry, modifier)`, `EmptyState(message, modifier)`

- [ ] **Step 1: Create `ExpiryBadge.kt`**

Stateless composable. `enum class ExpiryStatus { USE_TODAY, USE_SOON, NORMAL }`. `ExpiryBadge(status, label, modifier)` renders colored chip: Red (`MaterialTheme.colorScheme.error`) for USE_TODAY, Yellow (`MaterialTheme.colorScheme.tertiary`) for USE_SOON, Gray (`MaterialTheme.colorScheme.surfaceVariant`) for NORMAL. Preview all 3 states.

- [ ] **Step 2: Create `FoodItemCard.kt`**

Data class `FoodItemUi(id, name, expiryDate, expiryStatus, qtyDisplay, unitDisplay)`. Composable `FoodItemCard(food, onClick, modifier)` showing icon placeholder, name, quantity, expiry badge. Preview.

- [ ] **Step 3: Create `RecipeCard.kt`**

Data class `RecipeCardUi(id, name, imageUrl, category, servings, time, difficulty, likeCount, isFavorite)`. Composable `RecipeCard(recipe, onClick, onFavoriteClick?, modifier)` showing image (Coil), title, meta row, optional heart icon. Preview.

- [ ] **Step 4: Create `LoadingState.kt`, `ErrorState.kt`, `EmptyState.kt`**

Three small composables: `LoadingState(modifier)` = centered `CircularProgressIndicator`. `ErrorState(message, onRetry, modifier)` = centered text + "Thử lại" button. `EmptyState(message, icon?, modifier)` = centered icon + text. All with previews.

- [ ] **Step 5: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add shared UI components — ExpiryBadge, FoodItemCard, RecipeCard, states"
```

---

## Task 6: Inventory Screen (Default Landing)

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/inventory/InventoryScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/inventory/InventoryViewModel.kt`

**Interfaces:**
- Consumes: `FridgeRepository.getMyFridges()`, `FridgeRepository.getFoods()`, `FridgeRepository.getStats()`, `ExpiryBadge`, `FoodItemCard`, `LoadingState`, `ErrorState`
- Produces: `InventoryScreen(onNavigateToStorage, onNavigateToRecipes, onNavigateToSettings, modifier)`, `InventoryViewModel`

- [ ] **Step 1: Create `InventoryViewModel.kt`**

`@HiltViewModel` with `FridgeRepository` injected. `StateFlow<InventoryUiState>` containing: `fridgeName`, `stats: FridgeStatsResponse?`, `freezerFoods: List<FoodItemUi>`, `fridgeFoods: List<FoodItemUi>`, `isLoading`, `error`. On init, loads fridge list → picks first fridge → loads stats + foods. Methods: `refresh()`, `currentFridgeId: String`.

- [ ] **Step 2: Create `InventoryScreen.kt`**

TopBar: search icon (left), fridge name (center), settings icon (right). Below: 4 stat cards row (In Stock, Use Today, Use Soon, Clean Out) color-coded per §12. Freezer section with food items (tappable → `onNavigateToStorage("freezer")`). Fridge section similarly. Bottom floating "Nấu ăn" button → `onNavigateToRecipes`. Handles loading/error/empty states. Preview.

- [ ] **Step 3: Wire into `NavGraph`**

Add `composable<Route.Inventory>` calling `InventoryScreen` with navigation callbacks.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — app launches to Inventory with stat cards

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Inventory dashboard screen with stats and food sections"
```

---

## Task 7: Storage List Screen

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/storagelist/StorageListScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/storagelist/StorageListViewModel.kt`

**Interfaces:**
- Consumes: `FridgeRepository.getFoods(fridgeId, location)`, `FoodItemCard`, `ExpiryBadge`
- Produces: `StorageListScreen(location: String, onBack, modifier)`, `StorageListViewModel`

- [ ] **Step 1: Create `StorageListViewModel.kt`**

`@HiltViewModel`. Takes `location` from SavedStateHandle. Loads foods filtered by location. `StateFlow<StorageListUiState>` with `foods: List<FoodItemUi>`, `title` ("Tủ đông" or "Tủ lạnh"), `isLoading`, `error`.

- [ ] **Step 2: Create `StorageListScreen.kt`**

TopBar with back arrow and title. `LazyColumn` of `FoodItemCard` items with `key = { food.id }`. Each card shows icon, name, expiry date, color-coded badge. Supports empty state. Preview.

- [ ] **Step 3: Wire into `NavGraph`**

Add `composable<Route.StorageList>` with location argument.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — navigating from Inventory freezer/fridge section shows filtered list

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add StorageList screen for filtered food display"
```

---

## Task 8: Recipe List & Recipe Detail Screens

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/recipes/RecipeListScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/recipes/RecipeListViewModel.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/recipes/RecipeDetailScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/recipes/RecipeDetailViewModel.kt`

**Interfaces:**
- Consumes: `RecipeRepository`, `FavoriteRepository`, `RecipeCard`, `LoadingState`, `ErrorState`
- Produces: `RecipeListScreen(fridgeId, onRecipeClick, onBack, modifier)`, `RecipeDetailScreen(recipeId, fridgeId?, onBack, modifier)`

- [ ] **Step 1: Create `RecipeListViewModel.kt`**

Loads `RecipeRepository.getMatchedRecipes(fridgeId, limit=10)`. Search bar triggers `RecipeRepository.searchRecipes(category?, method?, name?)`. `StateFlow<RecipeListUiState>` with `recipes`, `isLoading`, `error`, `searchQuery`, `selectedCategory`, `selectedMethod`.

- [ ] **Step 2: Create `RecipeListScreen.kt`**

TopBar with back + "Gợi ý món ăn" title. Search bar with filter chips (Category, Cooking Method). `LazyColumn` of `RecipeCard` items. Tapping card → `onRecipeClick(recipeId)`. Preview.

- [ ] **Step 3: Create `RecipeDetailViewModel.kt`**

Loads `RecipeRepository.getRecipeDetail(recipeId, fridgeId?)`. `StateFlow<RecipeDetailUiState>` with `info`, `ingredients`, `detail`, `haveNeed`, `isLoading`, `error`. Methods: `toggleFavorite()` using `FavoriteRepository`.

- [ ] **Step 4: Create `RecipeDetailScreen.kt`**

Image header (Coil), title, meta row (servings, time, difficulty). Ingredients list. Step-by-step instructions. Two buttons: "Có sẵn" (Have) and "Cần mua" (Need) opening `ModalBottomSheet` per §12 invariant #8. Bottom sheet shows matched/missing ingredients. Uses `rememberSaveable` for sheet open state + `rememberModalBottomSheetState()`. Preview.

- [ ] **Step 5: Wire into `NavGraph`**

Add `composable<Route.RecipeList>` and `composable<Route.RecipeDetail>`.

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Recipe list loads from Inventory "Nấu ăn", detail opens with Have/Need sheets

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add RecipeList and RecipeDetail screens with Have/Need bottom sheets"
```

---

## Task 9: Reels Screen

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/reels/ReelsScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/reels/ReelsViewModel.kt`

**Interfaces:**
- Consumes: `RecipeRepository.getReels()`, `FavoriteRepository.likeRecipe()`, ExoPlayer
- Produces: `ReelsScreen(onNavigateToRecipeDetail, modifier)`, `ReelsViewModel`

- [ ] **Step 1: Create `ReelsViewModel.kt`**

Loads shuffled reels via `RecipeRepository.getReels()`. `StateFlow<ReelsUiState>` with `reels: List<ReelUi>`, `isLoading`, `error`. `ReelUi(id, name, servings, time, difficulty, videoUrl, likeCount, isLiked)`. Methods: `likeReel(id)`, `saveReel(id)` (sync to Favorite per §12 invariant #9).

- [ ] **Step 2: Create `ReelsScreen.kt`**

Full-screen vertical pager (`VerticalPager` or `LazyColumn` with `pagerSnapDistance`). Each page = video player (ExoPlayer `AndroidView`) + overlay with title, meta, like button, save button, "Nấu ngay" button. "Nấu ngay" → `onNavigateToRecipeDetail(reelId)` per §12 invariant #7. Like/save syncs to Favorite. Preview (static, no video).

- [ ] **Step 3: Wire into `NavGraph`**

Add `composable<Route.Reels>`.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Reels tab shows video cards with actions

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Reels video feed screen with ExoPlayer"
```

---

## Task 10: Favorite Screen

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/favorite/FavoriteScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/favorite/FavoriteViewModel.kt`

**Interfaces:**
- Consumes: `FavoriteRepository.getFavorites()`, `FavoriteRepository.unlikeRecipe()`, `RecipeCard`
- Produces: `FavoriteScreen(onRecipeClick, modifier)`, `FavoriteViewModel`

- [ ] **Step 1: Create `FavoriteViewModel.kt`**

Loads `FavoriteRepository.getFavorites()`. `StateFlow<FavoriteUiState>` with `favorites: List<RecipeCardUi>`, `isLoading`, `error`. Methods: `removeFavorite(recipeId)`, `refresh()`.

- [ ] **Step 2: Create `FavoriteScreen.kt`**

TopBar "Yêu thích". `LazyVerticalGrid` (2 columns) of `RecipeCard`. Tapping → `onRecipeClick(recipeId)`. Supports empty state "Chưa có món yêu thích". Preview.

- [ ] **Step 3: Wire into `NavGraph`**

Add `composable<Route.Favorite>`.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Favorite screen with recipe grid"
```

---

## Task 11: Scan Wizard (3-Step Flow)

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/scan/ScanScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/scan/ConfirmItemsScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/scan/AssignStorageScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/scan/ScanViewModel.kt`

**Interfaces:**
- Consumes: `ScanRepository`, `FridgeRepository` (for `fridgeId`)
- Produces: `ScanScreen(onNextStep, onCancel, modifier)`, `ConfirmItemsScreen(onNextStep, onBack, modifier)`, `AssignStorageScreen(onComplete, onBack, modifier)`, `ScanViewModel` (shared across 3 steps)

- [ ] **Step 1: Create `ScanViewModel.kt`**

`@HiltViewModel` managing wizard state machine: `enum ScanStep { SCAN, CONFIRM, ASSIGN }`. State: `scanId`, `scannedItems: List<ScannedItemUi>`, `currentStep`, `isLoading`, `error`. Methods:
- `createScanFromOcr(items)` → calls `POST /scans` → moves to CONFIRM
- `updateItem(itemId, updates)` → local state update
- `addManualItem(item)` → adds to list
- `confirmItems()` → calls `PUT /scans/:id/items` → moves to ASSIGN
- `assignLocation(itemId, location)` → updates location locally
- `commitScan()` → calls `PUT /scans/:id/locations` then `POST /scans/:id/commit` → returns to Inventory
- Per §12 invariant #3: commit only after all items have locations assigned

- [ ] **Step 2: Create `ScanScreen.kt`**

Camera preview (CameraX) with capture button + gallery picker. On capture/select → calls OCR service (or mock for now). Shows loading during OCR. On success → calls `viewModel.createScanFromOcr()`. No bottom nav visible.

- [ ] **Step 3: Create `ConfirmItemsScreen.kt`**

TopBar "Xác nhận thực phẩm" with back. `LazyColumn` showing each item: icon, name, quantity, purchase date, expiry date. Pencil icon per item for inline edit. "+ Thêm thực phẩm" button at bottom. "Xác nhận" button. All editable fields inline. Preview.

- [ ] **Step 4: Create `AssignStorageScreen.kt`**

TopBar "Phân loại" with back. Two target zones: "Tủ đông" (Freezer) and "Tủ lạnh" (Fridge). Unassigned items list with drag handles. Drag-and-drop via `Modifier.draggable()` or simpler tap-to-assign UX. "Hoàn tất" button disabled until all items assigned (§12 invariant #3, #4). Preview.

- [ ] **Step 5: Wire into `NavGraph`**

Add `composable<Route.Scan>`, `composable<Route.ConfirmItems>`, `composable<Route.AssignStorage>`. Use `hiltViewModel()` with shared backstack entry for `ScanViewModel`.

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — Scan tab launches camera, wizard progresses through 3 steps

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add 3-step scan wizard — capture, confirm, assign storage"
```

---

## Task 12: Family Screen

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/family/FamilyScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/family/FamilyViewModel.kt`

**Interfaces:**
- Consumes: `FridgeRepository.getMembers()`, `FridgeRepository.joinFridge()`
- Produces: `FamilyScreen(modifier)`, `FamilyViewModel`

- [ ] **Step 1: Create `FamilyViewModel.kt`**

Loads members via `FridgeRepository.getMembers(fridgeId)`. State: `members: List<MemberUi>`, `inviteCode`, `isLoading`, `error`. Methods: `joinWithCode(code)`, `refresh()`.

- [ ] **Step 2: Create `FamilyScreen.kt`**

TopBar "Gia đình". Invite code display section (shareable). Join section with code input + join button. Members list showing name, email, role badge (owner/member). `AlertDialog` for join confirmation. Preview.

- [ ] **Step 3: Wire into `NavGraph`**

Add `composable<Route.Family>`.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Family screen with members and invite code"
```

---

## Task 13: Login & Setup Fridge Screens

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/login/LoginScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/login/LoginViewModel.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/setup/SetupFridgeScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/setup/SetupFridgeViewModel.kt`

**Interfaces:**
- Consumes: `AuthRepository`, `FridgeRepository`
- Produces: `LoginScreen(onLoginSuccess, modifier)`, `SetupFridgeScreen(onSetupComplete, modifier)`

- [ ] **Step 1: Create `LoginViewModel.kt`**

Methods: `signInWithGoogle()`, `signInWithEmail(email, password)`, `signUp(email, password)`, `resetPassword(email)`. State: `isLoading`, `error`, `isAuthenticated`.

- [ ] **Step 2: Create `LoginScreen.kt`**

Logo + app name. Email/password fields. "Đăng nhập" button. "Đăng ký" toggle. Google + Facebook sign-in buttons. "Quên mật khẩu" link. No bottom nav. Preview.

- [ ] **Step 3: Create `SetupFridgeViewModel.kt`**

Method: `createFridge(name)` → creates fridge via Supabase direct call → navigates to Inventory. State: `name`, `isLoading`, `error`.

- [ ] **Step 4: Create `SetupFridgeScreen.kt`**

Illustration + "Đặt tên tủ lạnh" prompt. Text field. "Tiếp tục" button. No bottom nav. Preview.

- [ ] **Step 5: Update auth flow in `FridgeWiseApp.kt`**

On launch: check auth → if unauthenticated → Login. After login: check if user has fridges → if no → SetupFridge. After setup → Inventory (§12 invariant #1).

- [ ] **Step 6: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — cold launch shows Login → Setup → Inventory flow

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add Login and SetupFridge screens with auth flow"
```

---

## Task 14: Settings Screen

**Files:**
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/settings/SettingsScreen.kt`
- Create: `app/src/main/kotlin/com/production/fridgewise/ui/screens/settings/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `AuthRepository.signOut()`, `FridgeRepository.renameFridge()`
- Produces: `SettingsScreen(onBack, onLogout, modifier)`, `SettingsViewModel`

- [ ] **Step 1: Create `SettingsViewModel.kt`**

State: `userName`, `email`, `fridgeName`, `isLoading`. Methods: `renameFridge(newName)`, `logout()`.

- [ ] **Step 2: Create `SettingsScreen.kt`**

TopBar "Cài đặt" with back. Sections: Account (name, email), Fridge (rename with `AlertDialog`), Preferences (Language, Family link, Notification toggle), Help (Feedback, Data sources). "Đăng xuất" at bottom. Preview.

- [ ] **Step 3: Wire into `NavGraph`**

Add `composable<Route.Settings>`.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Settings screen with account, fridge rename, and logout"
```

---

## Task 15: Integration & Polish

**Files:**
- Modify: various files for wiring and bug fixes
- Modify: `ui/theme/Color.kt`, `ui/theme/Type.kt` (if design tokens from Figma are available)

**Interfaces:**
- Consumes: all screens and navigation from Tasks 4–14
- Produces: fully integrated, buildable app

- [ ] **Step 1: Verify all navigation paths per §12 invariants**

Trace each invariant manually in the nav graph:
1. Default landing = Inventory ✓
2. Bottom nav order: Reels → Favorite → Scan → Family → Inventory ✓
3. Scan wizard complete-only-at-Hoàn-tất ✓
4. Every food has location ✓
5. Expiry color coding ✓
6. "Nấu ăn" → Recipe List ✓
7. "Nấu ngay" → Recipe Detail ✓
8. Have/Need as bottom sheets ✓
9. Reel-Favorite sync ✓
10. Family = centralized member management ✓

- [ ] **Step 2: Add global 401 interceptor handling**

In `AuthInterceptor`, on 401 response → emit event via `SharedFlow` → `FridgeWiseApp` observes and navigates to Login.

- [ ] **Step 3: Add pull-to-refresh on Inventory, StorageList, RecipeList, Favorite**

Use `pullToRefresh` modifier from Material3.

- [ ] **Step 4: Full build and smoke test**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — complete app with all screens navigable

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: integration polish — 401 handling, pull-to-refresh, invariant verification"
```
