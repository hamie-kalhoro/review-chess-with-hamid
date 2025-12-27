// // Initialize the application
// let chessBoard;
// let currentGameAnalysis = null;
// let currentMoveIndex = -1;
//
// document.addEventListener('DOMContentLoaded', () => {
//     chessBoard = new ChessBoard('chessBoard');
//     initializeTheme();
//     initializeEventListeners();
//     updateEvalBar(0, null);
// });
//
// function initializeTheme() {
//     const savedTheme = localStorage.getItem('theme') || 'light';
//     const savedBoardTheme = localStorage.getItem('boardTheme') || 'classic';
//
//     document.body.dataset.theme = savedTheme;
//     document.body.dataset.boardTheme = savedBoardTheme;
//     updateThemeIcon();
// }
//
// function updateThemeIcon() {
//     const icon = document.querySelector('#themeToggle i');
//     if (document.body.dataset.theme === 'dark') {
//         icon.className = 'fas fa-sun';
//     } else {
//         icon.className = 'fas fa-moon';
//     }
// }
//
// function initializeEventListeners() {
//     // Theme toggle
//     document.getElementById('themeToggle').addEventListener('click', () => {
//         const currentTheme = document.body.dataset.theme;
//         document.body.dataset.theme = currentTheme === 'dark' ? 'light' : 'dark';
//         localStorage.setItem('theme', document.body.dataset.theme);
//         updateThemeIcon();
//     });
//
//     // Board theme selection
//     document.querySelectorAll('#boardThemes a').forEach(link => {
//         link.addEventListener('click', (e) => {
//             e.preventDefault();
//             const theme = e.target.dataset.theme;
//             document.body.dataset.boardTheme = theme;
//             localStorage.setItem('boardTheme', theme);
//             chessBoard.render();
//         });
//     });
//
//     // Tab switching
//     document.querySelectorAll('.tab-btn').forEach(btn => {
//         btn.addEventListener('click', () => {
//             const tabId = btn.dataset.tab;
//
//             document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
//             document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
//
//             btn.classList.add('active');
//             document.getElementById(tabId).classList.add('active');
//         });
//     });
//
//     // FEN input change - update board
//     document.getElementById('fenInput').addEventListener('input', (e) => {
//         try {
//             const fen = e.target.value.trim();
//             if (fen) {
//                 chessBoard.setPosition(fen);
//             }
//         } catch (err) {
//             console.log('Invalid FEN');
//         }
//     });
//
//     // Analyze FEN button
//     document.getElementById('analyzeFen').addEventListener('click', analyzePosition);
//
//     // Analyze Game button
//     document.getElementById('analyzeGame').addEventListener('click', analyzeGame);
//
//     // Navigation buttons
//     document.getElementById('firstMove').addEventListener('click', () => goToMove(0));
//     document.getElementById('prevMove').addEventListener('click', () => goToMove(currentMoveIndex - 1));
//     document.getElementById('nextMove').addEventListener('click', () => goToMove(currentMoveIndex + 1));
//     document.getElementById('lastMove').addEventListener('click', () => {
//         if (currentGameAnalysis) goToMove(currentGameAnalysis.moves.length - 1);
//     });
//
//     // Keyboard navigation
//     document.addEventListener('keydown', (e) => {
//         if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
//         if (e.key === 'ArrowLeft') goToMove(currentMoveIndex - 1);
//         if (e.key === 'ArrowRight') goToMove(currentMoveIndex + 1);
//         if (e.key === 'Home') goToMove(0);
//         if (e.key === 'End' && currentGameAnalysis) goToMove(currentGameAnalysis.moves.length - 1);
//     });
// }
//
// async function analyzePosition() {
//     const fen = document.getElementById('fenInput').value.trim();
//     const depth = parseInt(document.getElementById('depthInput').value) || 15;
//
//     if (!fen) {
//         alert('Please enter a valid FEN position');
//         return;
//     }
//
//     showLoading();
//
//     try {
//         const response = await fetch(`/api/chess/analyze?fen=${encodeURIComponent(fen)}&depth=${depth}&mode=bestmove`);
//         const data = await response.json();
//
//         hideLoading();
//
//         if (data.success) {
//             displayPositionAnalysis(data);
//             chessBoard.setPosition(fen);
//
//             if (data.bestMove) {
//                 const squares = algebraicToSquares(data.bestMove);
//                 chessBoard.highlightMove(squares.from, squares.to, 'Best');
//             }
//
//             updateEvalBar(data.evaluation, data.mate);
//         } else {
//             showError(data.message || 'Analysis failed');
//         }
//     } catch (err) {
//         hideLoading();
//         showError('Failed to connect to server: ' + err.message);
//     }
// }
//
// async function analyzeGame() {
//     const pgn = document.getElementById('pgnInput').value.trim();
//     const depth = parseInt(document.getElementById('pgnDepthInput').value) || 15;
//
//     if (!pgn) {
//         alert('Please enter a valid PGN');
//         return;
//     }
//
//     showLoading();
//
//     try {
//         const response = await fetch('/api/chess/analyze-game', {
//             method: 'POST',
//             headers: { 'Content-Type': 'application/json' },
//             body: JSON.stringify({ pgn, depth })
//         });
//         const data = await response.json();
//
//         hideLoading();
//
//         if (data.success) {
//             currentGameAnalysis = data;
//             currentMoveIndex = -1;
//             displayGameAnalysis(data);
//             document.getElementById('movesPanel').style.display = 'block';
//             goToMove(0);
//         } else {
//             showError(data.message || 'Game analysis failed');
//         }
//     } catch (err) {
//         hideLoading();
//         showError('Failed to analyze game: ' + err.message);
//     }
// }
//
// function displayPositionAnalysis(data) {
//     const content = document.getElementById('resultContent');
//
//     let evalText = 'N/A';
//     if (data.mate !== null && data.mate !== undefined) {
//         evalText = `Mate in ${Math.abs(data.mate)}`;
//     } else if (data.evaluation !== null && data.evaluation !== undefined) {
//         const evalValue = data.evaluation / 100;
//         evalText = (evalValue >= 0 ? '+' : '') + evalValue.toFixed(2);
//     }
//
//     content.innerHTML = `
//         <div class="result-item">
//             <span class="label">Best Move</span>
//             <span class="value">${data.bestMove || 'N/A'}</span>
//         </div>
//         <div class="result-item">
//             <span class="label">Evaluation</span>
//             <span class="value">${evalText}</span>
//         </div>
//         ${data.ponder ? `
//         <div class="result-item">
//             <span class="label">Ponder</span>
//             <span class="value">${data.ponder}</span>
//         </div>
//         ` : ''}
//         ${data.continuation ? `
//         <div class="result-item">
//             <span class="label">Continuation</span>
//             <span class="value" style="font-size: 0.8rem;">${data.continuation}</span>
//         </div>
//         ` : ''}
//     `;
// }
//
// function displayGameAnalysis(data) {
//     const movesList = document.getElementById('movesList');
//     const summary = document.getElementById('gameSummary');
//
//     let movesHtml = '';
//     for (let i = 0; i < data.moves.length; i += 2) {
//         const whiteMove = data.moves[i];
//         const blackMove = data.moves[i + 1];
//         const moveNum = Math.floor(i / 2) + 1;
//
//         movesHtml += `<span class="move-number">${moveNum}.</span>`;
//         movesHtml += `
//             <div class="move-item" data-index="${i}">
//                 <span class="classification-dot ${(whiteMove.classification || '').toLowerCase()}"></span>
//                 <span>${whiteMove.san || whiteMove.move}</span>
//             </div>
//         `;
//
//         if (blackMove) {
//             movesHtml += `
//                 <div class="move-item" data-index="${i + 1}">
//                     <span class="classification-dot ${(blackMove.classification || '').toLowerCase()}"></span>
//                     <span>${blackMove.san || blackMove.move}</span>
//                 </div>
//             `;
//         } else {
//             movesHtml += '<div></div>';
//         }
//     }
//     movesList.innerHTML = movesHtml;
//
//     document.querySelectorAll('.move-item').forEach(item => {
//         item.addEventListener('click', () => {
//             const index = parseInt(item.dataset.index);
//             goToMove(index);
//         });
//     });
//
//     if (data.summary) {
//         const s = data.summary;
//         summary.innerHTML = `
//             <h4>Game Summary</h4>
//             <div class="player-stats">
//                 <h5><i class="fas fa-chess-king" style="color: #fff; text-shadow: 0 0 2px #000;"></i> White</h5>
//                 <div class="accuracy-bar">
//                     <div class="accuracy-fill" style="width: ${s.whiteAccuracy || 50}%;"></div>
//                 </div>
//                 <div class="stats-grid">
//                     <div class="stat-item"><span class="dot" style="background: var(--brilliant);"></span>${s.whiteBrilliant || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--great);"></span>${s.whiteGreat || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--best);"></span>${s.whiteBest || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--inaccuracy);"></span>${s.whiteInaccuracy || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--mistake);"></span>${s.whiteMistake || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--blunder);"></span>${s.whiteBlunder || 0}</div>
//                 </div>
//             </div>
//             <div class="player-stats">
//                 <h5><i class="fas fa-chess-king" style="color: #000;"></i> Black</h5>
//                 <div class="accuracy-bar">
//                     <div class="accuracy-fill" style="width: ${s.blackAccuracy || 50}%;"></div>
//                 </div>
//                 <div class="stats-grid">
//                     <div class="stat-item"><span class="dot" style="background: var(--brilliant);"></span>${s.blackBrilliant || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--great);"></span>${s.blackGreat || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--best);"></span>${s.blackBest || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--inaccuracy);"></span>${s.blackInaccuracy || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--mistake);"></span>${s.blackMistake || 0}</div>
//                     <div class="stat-item"><span class="dot" style="background: var(--blunder);"></span>${s.blackBlunder || 0}</div>
//                 </div>
//             </div>
//         `;
//     }
// }
//
// function goToMove(index) {
//     if (!currentGameAnalysis || !currentGameAnalysis.moves) return;
//
//     const moves = currentGameAnalysis.moves;
//     if (index < 0) index = 0;
//     if (index >= moves.length) index = moves.length - 1;
//
//     currentMoveIndex = index;
//     const move = moves[index];
//
//     if (move.fen) {
//         chessBoard.setPosition(move.fen);
//     }
//
//     if (move.uci || move.bestMove) {
//         const uciMove = move.uci || move.bestMove;
//         const squares = algebraicToSquares(uciMove);
//         chessBoard.highlightMove(squares.from, squares.to, move.classification);
//     }
//
//     const evaluation = move.evaluation || move.evaluationAfter || 0;
//     updateEvalBar(evaluation, move.mate);
//
//     document.getElementById('moveCounter').textContent = `${index + 1} / ${moves.length}`;
//
//     document.querySelectorAll('.move-item').forEach(item => {
//         item.classList.toggle('active', parseInt(item.dataset.index) === index);
//     });
//
//     const activeMove = document.querySelector('.move-item.active');
//     if (activeMove) {
//         activeMove.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
//     }
// }
//
// function updateEvalBar(evaluation, mate) {
//     const evalFill = document.getElementById('evalFill');
//     const evalText = document.getElementById('evalText');
//
//     let percentage = 50;
//     let displayText = '0.0';
//
//     if (mate !== null && mate !== undefined) {
//         percentage = mate > 0 ? 95 : 5;
//         displayText = `M${Math.abs(mate)}`;
//     } else if (evaluation !== null && evaluation !== undefined) {
//         const evalValue = typeof evaluation === 'number' ? evaluation / 100 : parseFloat(evaluation);
//         percentage = 50 + (Math.atan(evalValue / 2) / Math.PI) * 100;
//         percentage = Math.max(5, Math.min(95, percentage));
//         displayText = (evalValue >= 0 ? '+' : '') + evalValue.toFixed(1);
//     }
//
//     evalFill.style.height = `${percentage}%`;
//     evalText.textContent = displayText;
// }
//
// function showLoading() {
//     document.getElementById('loadingOverlay').classList.add('active');
// }
//
// function hideLoading() {
//     document.getElementById('loadingOverlay').classList.remove('active');
// }
//
// function showError(message) {
//     document.getElementById('resultContent').innerHTML = `
//         <p style="color: var(--blunder); text-align: center; padding: 1rem;">${message}</p>
//     `;
// }
