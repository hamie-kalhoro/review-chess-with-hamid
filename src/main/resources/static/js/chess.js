// // Chess piece Unicode characters
// const PIECES = {
//     'K': '♔', 'Q': '♕', 'R': '♖', 'B': '♗', 'N': '♘', 'P': '♙',
//     'k': '♚', 'q': '♛', 'r': '♜', 'b': '♝', 'n': '♞', 'p': '♟'
// };
//
// // Classification colors for highlights
// const CLASSIFICATION_COLORS = {
//     'Brilliant': { color: '#1baca6', symbol: '!!' },
//     'Great': { color: '#5c8bb0', symbol: '!' },
//     'Best': { color: '#96bc4b', symbol: '★' },
//     'Good': { color: '#96bc4b', symbol: '✓' },
//     'Book': { color: '#a88865', symbol: '📖' },
//     'Inaccuracy': { color: '#f7c631', symbol: '?!' },
//     'Mistake': { color: '#e6912c', symbol: '?' },
//     'Blunder': { color: '#ca3431', symbol: '??' },
//     'Miss': { color: '#db5c3d', symbol: '✗' }
// };
//
// class ChessBoard {
//     constructor(containerId) {
//         this.container = document.getElementById(containerId);
//         this.position = this.getStartingPosition();
//         this.highlightedSquares = { from: null, to: null };
//         this.currentClassification = null;
//         this.render();
//     }
//
//     getStartingPosition() {
//         return this.fenToPosition('rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1');
//     }
//
//     fenToPosition(fen) {
//         const position = {};
//         const fenParts = fen.split(' ');
//         const rows = fenParts[0].split('/');
//
//         for (let row = 0; row < 8; row++) {
//             let col = 0;
//             for (const char of rows[row]) {
//                 if (isNaN(char)) {
//                     const square = String.fromCharCode(97 + col) + (8 - row);
//                     position[square] = char;
//                     col++;
//                 } else {
//                     col += parseInt(char);
//                 }
//             }
//         }
//         return position;
//     }
//
//     setPosition(fen) {
//         this.position = this.fenToPosition(fen);
//         this.render();
//     }
//
//     highlightMove(from, to, classification) {
//         this.highlightedSquares = { from, to };
//         this.currentClassification = classification;
//         this.render();
//     }
//
//     clearHighlights() {
//         this.highlightedSquares = { from: null, to: null };
//         this.currentClassification = null;
//         this.render();
//     }
//
//     render() {
//         this.container.innerHTML = '';
//
//         for (let row = 0; row < 8; row++) {
//             for (let col = 0; col < 8; col++) {
//                 const square = String.fromCharCode(97 + col) + (8 - row);
//                 const isLight = (row + col) % 2 === 0;
//
//                 const squareDiv = document.createElement('div');
//                 squareDiv.className = `square ${isLight ? 'light' : 'dark'}`;
//                 squareDiv.dataset.square = square;
//
//                 // Check for highlights
//                 if (this.highlightedSquares.from === square) {
//                     squareDiv.classList.add('highlight-from');
//                     if (this.currentClassification && CLASSIFICATION_COLORS[this.currentClassification]) {
//                         squareDiv.style.setProperty('--current-move-color',
//                             CLASSIFICATION_COLORS[this.currentClassification].color + '80');
//                     }
//                 }
//
//                 if (this.highlightedSquares.to === square) {
//                     squareDiv.classList.add('highlight-to');
//                     if (this.currentClassification && CLASSIFICATION_COLORS[this.currentClassification]) {
//                         squareDiv.style.setProperty('--current-move-color',
//                             CLASSIFICATION_COLORS[this.currentClassification].color + '80');
//
//                         // Add classification badge
//                         const badge = document.createElement('div');
//                         badge.className = `move-classification-badge ${this.currentClassification.toLowerCase()}`;
//                         badge.textContent = CLASSIFICATION_COLORS[this.currentClassification].symbol;
//                         squareDiv.appendChild(badge);
//                     }
//                 }
//
//                 // Add piece
//                 if (this.position[square]) {
//                     const pieceSpan = document.createElement('span');
//                     pieceSpan.className = 'piece';
//                     pieceSpan.textContent = PIECES[this.position[square]];
//                     squareDiv.appendChild(pieceSpan);
//                 }
//
//                 this.container.appendChild(squareDiv);
//             }
//         }
//     }
// }
//
// // Convert algebraic notation to square coordinates
// function algebraicToSquares(move) {
//     // Handle UCI format (e.g., "e2e4")
//     if (move && move.length >= 4) {
//         return {
//             from: move.substring(0, 2),
//             to: move.substring(2, 4)
//         };
//     }
//     return { from: null, to: null };
// }
