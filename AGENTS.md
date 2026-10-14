# AGENTS.md — Quy tắc code Jetpack Compose (FridgeWise)

> Stack: Compose BOM + Material3, `minSdk 23`, `compileSdk 36`, theme `MyAppTheme`. Mọi code mới phải tuân thủ file này.

## 0. Bắt buộc trước khi code UI

- Trước mọi task vẽ màn hình/component/animation theo Figma, PHẢI gọi skill `ui-ux-pro-max` trước để lấy design intelligence (palette, typography, spacing, motion) rồi mới code.
- Không được bỏ qua với lý do "màn đơn giản".

## 1. State

- Hoist state về lowest common ancestor: composable lá stateless, nhận `value` + `onEvent`.
- `remember` chỉ giữ state qua recomposition; cần qua xoay màn hình / process death → `rememberSaveable`.
- Không dùng `ArrayList` / `mutableListOf()` làm state (không observable, không recompose). Dùng `State<List<T>>` + `listOf()`.
- Business logic → `ViewModel` + `collectAsStateWithLifecycle()`, không để trong Composable.

```kotlin
@Composable
fun HelloScreen() {
    var name by rememberSaveable { mutableStateOf("") }
    HelloContent(name = name, onNameChange = { name = it })
}
```

## 2. Signature Composable

- Tham số bắt buộc trước, optional sau; `modifier: Modifier = Modifier` là optional đầu tiên; lambda `content` để trailing.
- Modifier chính tên `modifier`; modifier con đặt `xxxModifier` (vd `headerModifier`).
- Event đặt tên `on` + động từ hiện tại: `onClick`, `onChange` (sai: `onChanged`).

## 3. UI / Modifier / Theme

- Thứ tự Modifier: layout trước (`fillMaxWidth`, `size`) → `padding` → `clickable` / `graphics`.
- Màu/chữ chỉ từ `MaterialTheme.colorScheme` / `typography`. Cấm hardcode `Color(...)` / `fontSize` lẻ ngoài `ui/theme`.
- Màn hình dùng `Scaffold` + `innerPadding` (app đã `enableEdgeToEdge()`).
- Mọi component public phải có `@Preview` bọc `MyAppTheme`.
- `LazyColumn` luôn có `key`; tránh scroll lồng nhau không cần thiết.

## 4. Animation

- 1 giá trị → `animate*AsState`; nhiều giá trị cùng lúc → `updateTransition`.
- Ẩn/hiện → `AnimatedVisibility`; đổi layout → `Crossfade(targetState, label = "...")`; đổi size → `animateContentSize`.
- Cấm `LaunchedEffect` để chạy animation.

```kotlin
var visible by remember { mutableStateOf(true) }
AnimatedVisibility(visible) { Text("Hello") }
```

## 5. Popup / Dialog / BottomSheet

- Chỉ dùng Material3: `AlertDialog`, `ModalBottomSheet`. Cấm `Dialog` raw / PopupWindow.
- Dialog: luôn xử lý `onDismissRequest` (bấm ngoài / back). State mở/đóng bằng `rememberSaveable`.
- BottomSheet: `rememberSaveable open` + `rememberBottomSheetState(SheetValue.Hidden)`; đóng bằng `scope.launch { sheetState.hide() }.invokeOnCompletion { if (!isVisible) open = false }`; cần `@OptIn(ExperimentalMaterial3Api::class)`.

```kotlin
if (openDialog) {
    AlertDialog(
        onDismissRequest = { openDialog = false },
        title = { Text("Title") },
        text = { Text("...") },
        confirmButton = { TextButton(onClick = { openDialog = false }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = { openDialog = false }) { Text("Dismiss") } }
    )
}
```

## 6. Hiệu năng

- Tính toán nặng bọc `remember` / `derivedStateOf`; list dùng `key`.
- Không tạo `MutableTransitionState` / object nặng trực tiếp trong composition mà không `remember`.

## 7. Cấu trúc file

- `ui/components/` (tái dùng) · `ui/screens/<ten>/` (1 route = 1 Screen + 1 ViewModel) · `ui/theme/` giữ nguyên.
- 1 file < ~300 dòng; vượt → tách component.

## 8. Nguồn sự thật (đọc thay vì hỏi lại)

- `AGENTS.md` (luật Compose) · `FridgeWise_Functional_Specification.md` (flow, §12 bất biến)
  · `API.md` (scan/inventory/recipes/favorites, qty_display vs qty_base) · Figma file
  `kysC6NThAMsDAqJcv6fnwy` (9 màn, page `0:1`).
- Font Figma: Plus Jakarta Sans → map sang `MaterialTheme.typography` (chưa bundle font).

| Màn | node-id | Spec § | API § |
|---|---|---|---|
| Tổng quan tủ lạnh (= Inventory splash) | `66:811` | §4 | §2 stats |
| Danh sách Inventory | `66:416` | §4.5 | §2 foods + stats |
| Quét hóa đơn | `66:758` | §8.1 | §1 POST /scans + OCR |
| Xác nhận kết quả OCR | `66:649` | §8.2 | §1 PUT items |
| Xác nhận thêm thực phẩm | `66:556` | §8.3 | §1 PUT locations + commit |
| Trung tâm cảnh báo | `66:278` | §4.2 | §2 stats |
| Reels món ăn | `66:199` | §6 | §3 reels + §4 fav |
| Kiểm tra nguyên liệu | `66:94` | §5.2 | §3 search |
| Chi tiết nấu ăn (+Have/Need) | `66:2` | §5.3–5.4 | §3 detail |
