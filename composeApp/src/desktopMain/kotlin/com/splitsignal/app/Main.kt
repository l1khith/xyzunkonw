package com.splitsignal.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.splitsignal.app.di.initKoin
import com.splitsignal.app.presentation.AppScreen
import com.splitsignal.app.presentation.LinkTab
import com.splitsignal.app.presentation.SplitSignalAppUiState
import com.splitsignal.app.ui.BattleshipScreen
import com.splitsignal.app.ui.MathPuzzleScreen
import com.splitsignal.app.ui.SplitSignalColors
import com.splitsignal.app.ui.SplitSignalTheme
import com.splitsignal.core.domain.BattleshipEngine
import com.splitsignal.core.domain.BattleshipGameState
import com.splitsignal.core.domain.CrossMathGameState
import com.splitsignal.core.domain.CrossMathGenerator
import com.splitsignal.core.domain.CrossMathSize
import com.splitsignal.core.domain.Fnv1a
import com.splitsignal.core.domain.GameMode
import com.splitsignal.core.domain.GameType
import com.splitsignal.core.domain.InMemorySyncPipe
import com.splitsignal.core.domain.KenKenGameState
import com.splitsignal.core.domain.KenKenGenerator
import com.splitsignal.core.domain.KenKenSize
import com.splitsignal.core.domain.MathGameState
import com.splitsignal.core.domain.MathMazeGameState
import com.splitsignal.core.domain.MathMazeGenerator
import com.splitsignal.core.domain.MathPuzzleEngine
import com.splitsignal.core.domain.MemoryMatrixGameState
import com.splitsignal.core.domain.MemoryMatrixGenerator
import com.splitsignal.core.domain.MindsnapGameState
import com.splitsignal.core.domain.MindsnapGenerator
import com.splitsignal.core.domain.PlayerRole
import com.splitsignal.core.domain.ProgressionState
import com.splitsignal.core.domain.SudokuDifficulty
import com.splitsignal.core.domain.SudokuGameState
import com.splitsignal.core.domain.SudokuGenerator
import com.splitsignal.core.domain.SudokuSize
import com.splitsignal.core.domain.TransportConnectionState
import java.awt.Dimension
import kotlinx.coroutines.launch

private data class DualDemoSession(
    val gameType: GameType,
    val sessionId: Int,
)

fun main() {
    initKoin()
    application {
        var demoSession by remember { mutableStateOf<DualDemoSession?>(null) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "SplitSignal — Cooperative Math Deduction",
            state = rememberWindowState(width = 1200.dp, height = 820.dp),
            onKeyEvent = { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                    exitApplication()
                    true
                } else {
                    false
                }
            },
        ) {
            LaunchedEffect(window) {
                window.minimumSize = Dimension(640, 560)
            }
            SplitSignalApp(
                onLaunchDualDesktopDemo = { gameType ->
                    val target = if (gameType == GameType.BATTLESHIP) {
                        GameType.BATTLESHIP
                    } else {
                        GameType.MATH
                    }
                    demoSession = DualDemoSession(
                        gameType = target,
                        sessionId = (demoSession?.sessionId ?: 0) + 1,
                    )
                },
            )
        }

        val activeDemo = demoSession
        if (activeDemo != null) {
            DualJudgeDemoWindows(
                session = activeDemo,
                onClose = { demoSession = null },
            )
        }
    }
}

@Composable
private fun DualJudgeDemoWindows(
    session: DualDemoSession,
    onClose: () -> Unit,
) {
    val demoScope = rememberCoroutineScope()
    val (hostTransport, clientTransport) = remember(session.sessionId) {
        InMemorySyncPipe.createPair()
    }

    val leftWindowState = rememberWindowState(
        width = 580.dp,
        height = 740.dp,
        position = WindowPosition(x = 40.dp, y = 80.dp),
    )
    val rightWindowState = rememberWindowState(
        width = 580.dp,
        height = 740.dp,
        position = WindowPosition(x = 640.dp, y = 80.dp),
    )

    when (session.gameType) {
        GameType.BATTLESHIP -> {
            val seed = remember(session.sessionId) {
                Fnv1a.hash32("JUDGE_DEMO_SHIP:${session.sessionId}")
            }
            val p1Engine = remember(session.sessionId) {
                BattleshipEngine(
                    localRole = PlayerRole.OPERATOR,
                    seed = seed,
                    vsAi = false,
                    transport = hostTransport,
                    scope = demoScope,
                )
            }
            val p2Engine = remember(session.sessionId) {
                BattleshipEngine(
                    localRole = PlayerRole.ANALYST,
                    seed = seed,
                    vsAi = false,
                    transport = clientTransport,
                    scope = demoScope,
                )
            }
            DisposableEffect(session.sessionId) {
                onDispose {
                    p1Engine.dispose()
                    p2Engine.dispose()
                    hostTransport.close()
                    clientTransport.close()
                }
            }

            val p1State by p1Engine.state.collectAsState()
            val p2State by p2Engine.state.collectAsState()

            Window(
                onCloseRequest = onClose,
                title = "Player 1 — Fleet Grid (Judge Demo)",
                state = leftWindowState,
            ) {
                SplitSignalTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize().background(SplitSignalColors.BgObsidian),
                        color = SplitSignalColors.BgObsidian,
                    ) {
                        BattleshipScreen(
                            state = p1State,
                            roomCode = "DEMO",
                            widthSizeClass = WindowWidthSizeClass.Compact,
                            onFire = { idx -> demoScope.launch { p1Engine.fireAt(idx) } },
                            onSonar = { idx -> demoScope.launch { p1Engine.sonarScan(idx) } },
                            onRestart = { demoScope.launch { p1Engine.restart() } },
                            onExit = onClose,
                        )
                    }
                }
            }

            Window(
                onCloseRequest = onClose,
                title = "Player 2 — Fleet Grid (Judge Demo)",
                state = rightWindowState,
            ) {
                SplitSignalTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize().background(SplitSignalColors.BgObsidian),
                        color = SplitSignalColors.BgObsidian,
                    ) {
                        BattleshipScreen(
                            state = p2State,
                            roomCode = "DEMO",
                            widthSizeClass = WindowWidthSizeClass.Compact,
                            onFire = { idx -> demoScope.launch { p2Engine.fireAt(idx) } },
                            onSonar = { idx -> demoScope.launch { p2Engine.sonarScan(idx) } },
                            onRestart = { demoScope.launch { p2Engine.restart() } },
                            onExit = onClose,
                        )
                    }
                }
            }
        }

        else -> {
            val seed = remember(session.sessionId) {
                Fnv1a.hash32("JUDGE_DEMO_MATH:${session.sessionId}")
            }
            val p1Engine = remember(session.sessionId) {
                MathPuzzleEngine(
                    role = PlayerRole.OPERATOR,
                    seed = seed,
                    transport = hostTransport,
                    scope = demoScope,
                )
            }
            val p2Engine = remember(session.sessionId) {
                MathPuzzleEngine(
                    role = PlayerRole.ANALYST,
                    seed = seed,
                    transport = clientTransport,
                    scope = demoScope,
                )
            }
            DisposableEffect(session.sessionId) {
                onDispose {
                    p1Engine.dispose()
                    p2Engine.dispose()
                    hostTransport.close()
                    clientTransport.close()
                }
            }

            val p1State by p1Engine.state.collectAsState()
            val p2State by p2Engine.state.collectAsState()
            val baseUiState = remember(session.sessionId) { buildBaseDemoUiState(seed) }

            Window(
                onCloseRequest = onClose,
                title = "Player 1 — Number Grid (Judge Demo)",
                state = leftWindowState,
            ) {
                SplitSignalTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize().background(SplitSignalColors.BgObsidian),
                        color = SplitSignalColors.BgObsidian,
                    ) {
                        MathPuzzleScreen(
                            uiState = baseUiState.copy(
                                gameMode = GameMode.LAN_HOST,
                                activeRole = PlayerRole.OPERATOR,
                                mathSolverState = p1State,
                                mathClueState = p2State,
                                connectedClients = 1,
                            ),
                            widthSizeClass = WindowWidthSizeClass.Compact,
                            onOpenCoopLobby = {},
                            onExitToHub = onClose,
                            onCellTapped = { idx -> demoScope.launch { p1Engine.onCellTapped(idx) } },
                            onNextLevel = { demoScope.launch { p1Engine.onNextLevel() } },
                        )
                    }
                }
            }

            Window(
                onCloseRequest = onClose,
                title = "Player 2 — Clue Sheet (Judge Demo)",
                state = rightWindowState,
            ) {
                SplitSignalTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize().background(SplitSignalColors.BgObsidian),
                        color = SplitSignalColors.BgObsidian,
                    ) {
                        MathPuzzleScreen(
                            uiState = baseUiState.copy(
                                gameMode = GameMode.LAN_CLIENT,
                                activeRole = PlayerRole.ANALYST,
                                mathSolverState = p1State,
                                mathClueState = p2State,
                                clientConnectionState = TransportConnectionState.CONNECTED,
                            ),
                            widthSizeClass = WindowWidthSizeClass.Compact,
                            onOpenCoopLobby = {},
                            onExitToHub = onClose,
                            onCellTapped = {},
                            onNextLevel = { demoScope.launch { p2Engine.onNextLevel() } },
                        )
                    }
                }
            }
        }
    }
}

private fun buildBaseDemoUiState(seed: UInt): SplitSignalAppUiState {
    val sudokuBoard = SudokuGenerator.generate(
        size = SudokuSize.SPRINT_6X6,
        difficulty = SudokuDifficulty.MEDIUM,
        seed = seed,
    )
    val crossBoard = CrossMathGenerator.generate(seed, CrossMathSize.GRID_3X3)
    val mazeBoard = MathMazeGenerator.generate(seed, 4)
    val kenKenBoard = KenKenGenerator.generate(seed, KenKenSize.GRID_4X4)
    val memoryBoard = MemoryMatrixGenerator.generate(seed, 4)
    val mindsnapDeck = MindsnapGenerator.generateDeck(seed)
    val emptyMath = MathGameState(
        role = PlayerRole.OPERATOR,
        levelIndex = 0,
        title = "Demo",
        grid = List(9) { 1 },
        clues = emptyList(),
        isSolved = false,
    )
    return SplitSignalAppUiState(
        screen = AppScreen.COOP_ACTIVE,
        gameMode = GameMode.LAN_HOST,
        selectedGameType = GameType.MATH,
        linkTab = LinkTab.HOST,
        canHostLan = true,
        nearbyRooms = emptyList(),
        hostInviteToken = "",
        progressionState = ProgressionState(),
        sudokuState = SudokuGameState(
            size = SudokuSize.SPRINT_6X6,
            difficulty = SudokuDifficulty.MEDIUM,
            seed = sudokuBoard.seed,
            givens = sudokuBoard.givens,
            grid = sudokuBoard.givens.toList(),
            solution = sudokuBoard.solution,
        ),
        crossMathState = CrossMathGameState(
            seed = crossBoard.seed,
            size = crossBoard.size,
            board = crossBoard,
            values = List(crossBoard.cellCount) { null },
            bank = emptyList(),
            selectedCellIndex = 0,
            rowSatisfied = List(crossBoard.dim) { false },
            colSatisfied = List(crossBoard.dim) { false },
            moves = 0,
            mistakes = 0,
            elapsedSeconds = 0,
            percentComplete = 0,
            isSolved = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        ),
        mathMazeState = MathMazeGameState(
            seed = mazeBoard.seed,
            board = mazeBoard,
            path = listOf(mazeBoard.startIndex),
            runningTotal = mazeBoard.startValue,
            hintedNextIndex = null,
            moves = 0,
            mistakes = 0,
            elapsedSeconds = 0,
            percentComplete = 0,
            isSolved = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        ),
        kenKenState = KenKenGameState(
            seed = kenKenBoard.seed,
            size = kenKenBoard.size,
            board = kenKenBoard,
            values = List(kenKenBoard.cellCount) { null },
            conflictCells = emptySet(),
            selectedCellIndex = 0,
            moves = 0,
            mistakes = 0,
            elapsedSeconds = 0,
            percentComplete = 0,
            isSolved = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        ),
        memoryMatrixState = MemoryMatrixGameState(
            seed = memoryBoard.seed,
            board = memoryBoard,
            isRevealing = false,
            selectedIndices = emptyList(),
            wrongFlashIndex = null,
            moves = 0,
            mistakes = 0,
            elapsedSeconds = 0,
            percentComplete = 0,
            isSolved = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        ),
        mindsnapState = MindsnapGameState(
            seed = seed,
            questions = mindsnapDeck,
            currentIndex = 0,
            score = 0,
            combo = 0,
            bestCombo = 0,
            multiplier = 1,
            remainingSeconds = MindsnapGenerator.BLITZ_DURATION_SECONDS,
            elapsedSeconds = 0,
            moves = 0,
            mistakes = 0,
            percentComplete = 0,
            isFinished = false,
            isRace = false,
            isDailyStep = false,
            opponentProgress = null,
        ),
        isTriathlonActive = false,
        triathlonRoundIndex = 0,
        isDailyWorkoutRun = false,
        lastSolveReward = null,
        selectedSudokuSize = SudokuSize.SPRINT_6X6,
        selectedSudokuDifficulty = SudokuDifficulty.MEDIUM,
        mathSolverState = emptyMath,
        mathClueState = emptyMath.copy(role = PlayerRole.ANALYST),
        battleshipState = BattleshipGameState(
            localRole = PlayerRole.OPERATOR,
            vsAi = false,
            myShips = emptySet(),
            enemyShips = emptySet(),
            myHitsReceived = emptySet(),
            myMissesReceived = emptySet(),
            enemyHits = emptySet(),
            enemyMisses = emptySet(),
            sonarRevealedShips = emptySet(),
            sonarScannedEmpty = emptySet(),
            sonarChargesLeft = 0,
            isMyTurn = true,
            winnerRole = null,
            statusMessage = "",
        ),
        activeRole = PlayerRole.OPERATOR,
        hostRole = PlayerRole.OPERATOR,
        hostRoomCode = "DEMO",
        hostAddress = null,
        connectedClients = 1,
        clientConnectionState = TransportConnectionState.CONNECTED,
        clientEndpointInput = "",
        clientRoomCodeInput = "DEMO",
        clientRole = PlayerRole.ANALYST,
        clientValidationError = null,
    )
}
