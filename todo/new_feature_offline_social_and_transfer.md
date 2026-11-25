# Feature Specification: Offline User ID, Friends Discovery, Live Chat & 1 GB Streaming File Transfer

## 1. Goals & Non-Negotiable Constraints
- **100% Offline / Zero Cloud:** All identity, friend handshakes, peer discovery, live chat, and file transfers run locally over Wi-Fi, mobile hotspot, or USB `adb reverse` — zero internet or external servers.
- **Zero-OOM 1 GB Streaming Transfers:** Files up to **1 GB (`1,073,741,824` bytes)** must stream disk-to-disk in **64 KB (`65,536` byte) chunks** over Ktor CIO HTTP (`ByteReadChannel` / `ByteWriteChannel`), keeping RAM usage flat (`< 5 MB`) and never buffering entire files in memory or WebSocket text frames.
- **Linear-Inspired Minimalist Dark UI:** `#08090A` canvas, `#101114` cards, `#17191E` tiles, `1.dp` `Color(0x14FFFFFF)` hairlines, `#F7F8F8` primary ink, `#8A8F98` secondary ink, `#5E6AD2` primary accent, `#26B57B` emerald online/success indicator, `fontFeatureSettings = "tnum"` on all IDs, file sizes, speeds, and counters, and zero sci-fi jargon.

---

## 2. Architecture Breakdown

### 2.1 Persistent Offline User Identity & Local Persistence (`:core:domain` + `expect/actual`)
- **`LocalStorageProvider` (`expect/actual`):** Thin platform-specific key-value / JSON file persistence in `jvmAndAndroidMain` (stored in app files dir / `~/.splitsignal/`) and `wasmJsMain` (in-memory fallback).
- **`UserIdentityStore` (`commonMain`):**
  - First launch generates a permanent 8-character ID (`KMP-XXXX`, uppercase alphanumeric from `DeterministicRng` / time-seeded entropy) and persists it alongside an editable `displayName` and `avatarAccentIndex`.
  - Persists the local **Friends Registry** (`FriendRecord`: `userId`, `displayName`, `addedAtEpochMs`, `lastSeenEpochMs`, `duelWins`, `duelLosses`).
  - Persists recent direct chat history per peer `userId` (`ChatMessageRecord`) so conversations survive app restarts.

### 2.2 Two-Bucket UDP LAN Discovery (`:core:domain` + `:core:network`)
- **Peer Presence Beacon (UDP Port `45678`, every `1000ms`):**
  - Payload: `SPLITSIGNAL_PEER|<userId>|<displayName>|<localIp>|<ktorPort>|<status>`
  - Backward-compatible with existing `SPLITSIGNAL|<roomCode>|<ip>|<port>|<gameType>` room beacons on the same UDP port.
- **`NearbyDevicesState` (`StateFlow` in `commonMain`):**
  - Evicts peers silent for `> 3500ms`.
  - Partitions peers into 3 clean lists:
    1. `friendsInRange`: Discovered peers whose `userId` is in `Friends Registry` (live `#26B57B` dot, quick actions: `Duel`, `Chat`, `Send File`).
    2. `unknownNearby`: Discovered peers not yet in `Friends Registry` (display name, `KMP-XXXX` ID, `+ Add Friend`, `Chat`, `Send File`).
    3. `savedFriendsOffline`: Saved friends not currently broadcasting.
- **Mutual Friend Handshake:** `+ Add Friend` sends `SyncPacket.FriendRequest(fromUserId, fromName)` over WebSocket; peer `Accept` replies with `SyncPacket.FriendAccept(fromUserId, fromName)` and both devices persist each other.

### 2.3 Direct P2P Live Chat (`SyncPacket` + `ChatSessionManager`)
- Extend `SyncPacket` with `@Serializable` social & transfer control frames:
  - `FriendRequest(fromUserId, fromName)`
  - `FriendAccept(fromUserId, fromName)`
  - `ChatTextMessage(messageId, senderId, senderName, text, timestampMs)`
  - `ChatTypingIndicator(senderId, isTyping)`
  - `FileTransferOffer(transferId, senderId, fileName, fileSizeBytes, mimeType)`
  - `FileTransferDecision(transferId, accepted, reason)`
  - `FileTransferProgress(transferId, bytesTransferred, totalBytes, speedBytesPerSec)`
- `ChatSessionManager` manages active peer WebSocket connection (persistent peer-to-peer chat/social Ktor CIO endpoint), typing state, incoming friend requests, chat history persistence, and file transfer state machine.

### 2.4 Streaming File Transfer Up to 1 GB (Control Plane vs. Data Plane)
- **Strict 1 GB Guard:** `MAX_FILE_TRANSFER_BYTES = 1_073_741_824L`. Rejects oversized files immediately with `"File exceeds the 1 GB offline transfer limit"`.
- **Control Plane (WebSocket):** Sender transmits `FileTransferOffer`. Receiver renders an inline accept/decline card in the chat thread with exact file name and formatted size (`842.5 MB`).
- **Data Plane (Ktor CIO HTTP Streaming):**
  - Once accepted, receiver pulls via `GET /api/transfer/{transferId}` (or sender pushes via `POST /api/transfer/{transferId}`) using `65_536`-byte (64 KB) chunked streaming (`PlatformFileHandle.openChunkedReader()` -> `ByteWriteChannel`, `ByteReadChannel` -> `PlatformFileHandle.openChunkedWriter()`).
  - Supports mid-stream **Cancel Transfer** via coroutine `Job.cancel()`.
  - Throttles progress updates every `250ms` (`412 MB / 850 MB · 22.4 MB/s` with `tnum`).
- **Platform File Picker & Saver (`expect/actual`):**
  - `PlatformFileBridge` in `commonMain` with `PlatformFileHandle(id, name, sizeBytes, mimeType)`.
  - `androidMain`: SAF `ActivityResultContracts.OpenDocument` + `MediaStore` / `Downloads/SplitSignal` output.
  - `desktopMain`: AWT/Swing `FileDialog` / `JFileChooser` + `~/Downloads/SplitSignal` output.
  - `wasmJsMain`: Graceful unsupported stub.

### 2.5 Minimalist 3-Tab UI Integration (`:composeApp`)
- **Bottom Navigation (3 Tabs):** `Play` · `Nearby` · `Profile`.
- **`Nearby` Tab:** 3-section layout (`Friends in Range`, `Unknown Nearby`, `Offline Friends`) with `16.dp` Material/Lucide-style vector icons (`PersonAdd`, `ChatBubbleOutline`, `AttachFile`, `SportsEsports`, `Wifi`).
- **Direct Chat & File Sheet:** `#08090A` message thread, `#5E6AD2` right-aligned sender bubbles, `#101114` left-aligned receiver bubbles, inline file offer/progress cards with `Cancel`, and single bottom bar (`[ 📎 ] [ Message... ] [ Send ]`).
- **`Profile` Tab:** Permanent `KMP-XXXX` ID with 1-tap `Copy -> Check` morph animation, editable display name, and head-to-head duel stats.

---

## 3. Implementation & Verification Checklist
- [x] Create and refine `todo/new_feature_offline_social_and_transfer.md`.
- [x] `:core:domain`: `UserIdentityStore`, `FriendRecord`, `NearbyDevicesState`, `SyncPacket` social/transfer frames, `ChatSessionManager`, `PlatformFileHandle` & `1 GB` guard + unit tests.
- [x] `:core:network`: Two-bucket UDP peer discovery (`SPLITSIGNAL_PEER|...`), Ktor CIO HTTP `GET/POST /api/transfer/{transferId}` 64 KB streaming endpoint & client streamer, `LocalStorageProvider` `actual`s.
- [x] `:composeApp`: 3-Tab navigation (`Play` · `Nearby` · `Profile`), `NearbyScreen`, `DirectChatSheet`, `ProfileScreen` (Copy -> Check morph, editable name), `PlatformFileBridge` (`androidMain` SAF + `desktopMain` FileDialog).
- [x] Challenge Flow (`GameInvite` + full-screen `GameInviteModal`), Global `FriendRequestModal`, Offline Friend history view, and Desktop Dual-Window Judge Demo Mode.
- [x] Verify `./gradlew :core:domain:desktopTest :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinDesktop :composeApp:compileKotlinWasmJs` passes with zero errors.

## 4. Behavioral Design & Gamification Integration
- [x] **The Invisible Scoreboard:** The `Nearby` tab shows the Elo, Streak, and Top Trophy of discovered friends.
- [x] **The Ceremony:** Winning a match triggers a 3-stage reward sequence (Anticipation -> Particle/Haptic Reveal -> Afterglow with Elo transition & Stats).
- [x] **Agency-Wrapped Streaks:** Daily activity closure ring starting at 25% (Goal Gradient) with Diamond Tier milestones and Streak Freeze agency.
- [x] **Competence Proofs:** Trophies reflect actual in-game achievements (`Flawless Clear`, `Speed Demon`, `Duel Victor`, `Diamond Streak`), not just app opens.
- [x] **IKEA Onboarding:** 4-step onboarding letting the user pick an accent color and solve one micro-puzzle before claiming `KMP-XXXX`.
- [x] **4-Tab Responsive AppShell:** Desktop `80.dp` Left Navigation Rail / Mobile `64.dp` Bottom Bar (`Play` · `Friends` · `Nearby` · `Profile`), Contextual `56.dp` Top Bar, and Global Slide-Down Notification Banner.
