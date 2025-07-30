    package com.example.app1;
    
    import androidx.core.view.WindowCompat;
    import androidx.core.view.WindowInsetsCompat;
    import androidx.core.view.WindowInsetsControllerCompat;
    
    import android.app.AlertDialog;
    import android.app.Dialog;
    import android.content.res.TypedArray;
    import android.graphics.Color;
    import android.graphics.drawable.ColorDrawable;
    import android.media.MediaPlayer;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.Point;
    import android.os.Bundle;
    import android.os.CountDownTimer;
    import android.os.Handler;
    import android.view.Window;
    import android.widget.Button;
    import android.widget.ImageButton;
    import android.widget.ProgressBar;
    import android.widget.TextView;
    import android.widget.Toast;
    import androidx.appcompat.app.AppCompatActivity; // Sử dụng AppCompatActivity để có nhiều tính năng hơn
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;
    
    public class GameActivity extends AppCompatActivity {
    
        // --- Cấu hình Game ---
        private static final int TOTAL_ROWS = 9;  // SỬA Ở ĐÂY
        private static final int TOTAL_COLS = 16;  // SỬA Ở ĐÂY
        private static final int TOTAL_POKEMON_TYPES = 18; // 9*16 = 144 ô => 72 cặp. 72 chia hết cho 18.
        private static final int GAME_TIME_IN_SECONDS = 900; //
        private static final int INITIAL_SHUFFLES = 10;
    
        // --- Các thành phần UI ---
        private TextView tvLevel, tvShuffleCount, tvScore;
        private ImageButton btnMute, btnShuffle, btnReplay;
        private ProgressBar timeProgressBar;
        private GameBoardView gameBoardView;
    
        // --- Trạng thái Game ---
        private int[][] board;
        private Bitmap[] pokemonImages;
        private int currentLevel = 1;
        private int currentScore = 0;
        private int shufflesLeft = INITIAL_SHUFFLES;
        private Point firstSelection = null;
        private int remainingPairs;
    
        private boolean isMuted = false;
        private MediaPlayer backgroundMusicPlayer;
        private final Handler handler = new Handler();
        private CountDownTimer gameTimer;
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            hideSystemUI();
    
            setContentView(R.layout.activity_game);
    
            mapUIComponents();
            setupButtonListeners();
    
            loadResources(); // Load tài nguyên trước
            startNewGame();
            playBackgroundMusic();
        }
    
        private void mapUIComponents() {
            tvLevel = findViewById(R.id.tv_level);
            tvShuffleCount = findViewById(R.id.tv_shuffle_count);
            tvScore = findViewById(R.id.tv_score);
            btnMute = findViewById(R.id.btn_mute);
            btnShuffle = findViewById(R.id.btn_shuffle);
            btnReplay = findViewById(R.id.btn_replay);
            timeProgressBar = findViewById(R.id.time_progress_bar);
            gameBoardView = findViewById(R.id.game_board_view);
        }
    
        private void setupButtonListeners() {
            btnShuffle.setOnClickListener(v -> handleShuffle());
            btnReplay.setOnClickListener(v -> restartGameFromBeginning());
            btnMute.setOnClickListener(v -> toggleMute());
        }
    
        private void playBackgroundMusic() {
            if (backgroundMusicPlayer == null) {
                backgroundMusicPlayer = MediaPlayer.create(this, R.raw.background_music);
                backgroundMusicPlayer.setLooping(true); // Lặp lại nhạc nền
            }
            if (!backgroundMusicPlayer.isPlaying() && !isMuted) {
                backgroundMusicPlayer.start();
            }
        }
    
        private void toggleMute() {
            isMuted = !isMuted;
            if (isMuted) {
                if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
                    backgroundMusicPlayer.pause();
                }
                // SỬA LẠI ICON CHO ĐÚNG: Khi Mute -> icon phải là loa bị gạch chéo
                btnMute.setImageResource(R.drawable.ic_mute_off);
                Toast.makeText(this, "Đã tắt âm thanh", Toast.LENGTH_SHORT).show();
            } else {
                if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying()) {
                    backgroundMusicPlayer.start();
                }
                // Khi không Mute -> icon là loa bình thường
                btnMute.setImageResource(R.drawable.ic_mute_on);
                Toast.makeText(this, "Đã bật âm thanh", Toast.LENGTH_SHORT).show();
            }
        }
    
        private void loadResources() {
            pokemonImages = new Bitmap[TOTAL_POKEMON_TYPES + 1]; // +1 vì index bắt đầu từ 1
    
            // Sử dụng TypedArray để load tài nguyên - nhanh, an toàn và chuyên nghiệp
            final TypedArray ids = getResources().obtainTypedArray(R.array.pokemon_drawables);
            for (int i = 0; i < ids.length(); i++) {
                int resourceId = ids.getResourceId(i, 0);
                if (resourceId != 0) {
                    // index của mảng pokemonImages bắt đầu từ 1, tương ứng với pokemon_1
                    pokemonImages[i + 1] = BitmapFactory.decodeResource(getResources(), resourceId);
                }
            }
            // Rất quan trọng: giải phóng TypedArray sau khi dùng xong
            ids.recycle();
    
            gameBoardView.setPokemonImages(pokemonImages);
        }
    
        private void startNewGame() {
            // Khởi tạo bàn cờ với viền trống xung quanh để thuật toán dễ hơn
            board = new int[TOTAL_ROWS + 2][TOTAL_COLS + 2];
            remainingPairs = (TOTAL_ROWS * TOTAL_COLS) / 2;
    
            // Tạo danh sách các cặp Pokemon
            List<Integer> pokemonIDs = new ArrayList<>();
            for (int i = 0; i < remainingPairs; i++) {
                // Sử dụng toán tử chia lấy dư để quay vòng qua các loại Pokemon
                int pokemonType = (i % TOTAL_POKEMON_TYPES) + 1; // +1 vì loại Pokemon bắt đầu từ 1
                pokemonIDs.add(pokemonType);
                pokemonIDs.add(pokemonType);
            }
            Collections.shuffle(pokemonIDs);
    
            // Đổ Pokemon vào bàn cờ
            int k = 0;
            for (int i = 1; i <= TOTAL_ROWS; i++) {
                for (int j = 1; j <= TOTAL_COLS; j++) {
                    board[i][j] = pokemonIDs.get(k++);
                }
            }
    
            // Cập nhật UI
            gameBoardView.setBoard(board);
            gameBoardView.setOnTileClickListener(this::handleTileClick);
            updateUI();
    
            // TODO: Bắt đầu đếm ngược thời gian
            startTimer();
        }
    
        private void startTimer() {
            // Hủy timer cũ nếu có
            if (gameTimer != null) {
                gameTimer.cancel();
            }
    
            timeProgressBar.setProgress(100); // Reset thanh thời gian
    
            gameTimer = new CountDownTimer(GAME_TIME_IN_SECONDS * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    // Cập nhật ProgressBar mỗi giây
                    int progress = (int) (millisUntilFinished * 100 / (GAME_TIME_IN_SECONDS * 1000));
                    timeProgressBar.setProgress(progress);
                }
    
                @Override
                public void onFinish() {
                    // Hết giờ
                    timeProgressBar.setProgress(0);
                    // THAY THẾ TOAST BẰNG HÀM GỌI DIALOG
                    showGameOverDialog();
                }
            };
    
            gameTimer.start();
        }

        private void showGameOverDialog() {
            // Tạo một đối tượng Dialog
            final Dialog dialog = new Dialog(this);

            // Bỏ đi tiêu đề mặc định của Dialog
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            // Gán layout tùy chỉnh vào Dialog
            dialog.setContentView(R.layout.dialog_game_over);

            // Làm cho nền của cửa sổ Dialog trong suốt để thấy được bo góc của layout
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            // Ánh xạ các nút từ layout tùy chỉnh
            Button btnReplay = dialog.findViewById(R.id.btn_dialog_replay);
            Button btnExit = dialog.findViewById(R.id.btn_dialog_exit);

            // Gán sự kiện cho nút "Chơi lại"
            btnReplay.setOnClickListener(v -> {
                dialog.dismiss(); // Đóng dialog trước
                restartGameFromBeginning();
            });

            // Gán sự kiện cho nút "Thoát Game"
            btnExit.setOnClickListener(v -> {
                dialog.dismiss(); // Đóng dialog trước
                finish(); // Đóng Activity
            });

            // Không cho phép hủy dialog bằng nút back hoặc chạm ra ngoài
            dialog.setCancelable(false);

            // Hiển thị dialog
            dialog.show();
        }

        private void handleTileClick(int row, int col) {
            // Nếu bàn cờ đang trong quá trình xử lý (firstSelection == null nhưng có path), không cho chọn
            if (board[row][col] == 0) return;

            if (firstSelection == null) {
                firstSelection = new Point(col, row);
                gameBoardView.setSelectedTile(firstSelection);
            } else {
                // Lần chọn thứ hai
                Point secondSelection = new Point(col, row);

                if(firstSelection.equals(secondSelection)) {
                    firstSelection = null;
                    gameBoardView.setSelectedTile(null);
                    return;
                }

                if (board[firstSelection.y][firstSelection.x] == board[secondSelection.y][secondSelection.x]) {
                    List<Point> path = findPath(firstSelection, secondSelection);
                    if (path != null) {
                        // ================================================================
                        // KHỐI LỆNH ĐÃ ĐƯỢC SỬA LỖI VÀ TỐI ƯU
                        // ================================================================

                        // BƯỚC 1: "Bắt giữ" trạng thái hiện tại vào các biến final
                        // Các biến này sẽ được lambda sử dụng một cách an toàn
                        final Point finalFirstSelection = firstSelection;
                        final Point finalSecondSelection = secondSelection;

                        // BƯỚC 2: Ngay lập tức xóa trạng thái lựa chọn để ngăn người dùng
                        // chạm vào các ô khác trong lúc hiệu ứng đang diễn ra.
                        firstSelection = null;
                        gameBoardView.setEnabled(false); // Vô hiệu hóa tạm thời View để không nhận chạm

                        // Vẽ đường nối
                        gameBoardView.drawPath(path);

                        // Delay một chút rồi mới xóa
                        handler.postDelayed(() -> {
                            // BƯỚC 3: Sử dụng các biến final đã được "bắt giữ" an toàn
                            board[finalFirstSelection.y][finalFirstSelection.x] = 0;
                            board[finalSecondSelection.y][finalSecondSelection.x] = 0;

                            // GỌI HÀM DI CHUYỂN BÀN CỜ
                            shiftBoard(finalFirstSelection, finalSecondSelection);

                            // Dọn dẹp và cập nhật UI
                            gameBoardView.clearPathAndSelection();
                            currentScore += 10;
                            remainingPairs--;
                            updateUI();

                            // Kiểm tra thắng
                            if (remainingPairs == 0) {
                                handleWin();
                            } else if (!isMoveAvailable()) {
                                Toast.makeText(this, "Không còn nước đi! Tự động xáo trộn.", Toast.LENGTH_SHORT).show();
                                handleShuffle();
                            }

                            gameBoardView.setEnabled(true); // Bật lại View sau khi xử lý xong

                        }, 300); // Tăng nhẹ delay để người dùng thấy đường nối

                    } else {
                        // Nối thất bại
                        firstSelection = null;
                        gameBoardView.setSelectedTile(null);
                    }
                } else {
                    // Chọn 2 ô khác loại
                    firstSelection = null;
                    gameBoardView.setSelectedTile(null);
                }
            }
        }

        private void handleShuffle() {
            if (shufflesLeft > 0) {
                shufflesLeft--;

                List<Integer> remainingPokemonIDs = new ArrayList<>();
                // SỬA Ở ĐÂY: Danh sách các ô có Pokémon, không phải tất cả các ô
                List<Point> occupiedSlots = new ArrayList<>();

                // Bước 1: Thu thập tất cả Pokémon còn lại và VỊ TRÍ của chúng
                for (int i = 1; i <= TOTAL_ROWS; i++) {
                    for (int j = 1; j <= TOTAL_COLS; j++) {
                        if (board[i][j] != 0) {
                            remainingPokemonIDs.add(board[i][j]);
                            occupiedSlots.add(new Point(j, i)); // Lưu lại vị trí của ô có Pokémon
                        }
                    }
                }

                // Bước 2: Xáo trộn danh sách các Pokémon
                Collections.shuffle(remainingPokemonIDs);

                // Bước 3: Đặt các Pokémon đã xáo trộn trở lại đúng các vị trí cũ
                for (int i = 0; i < occupiedSlots.size(); i++) {
                    Point slot = occupiedSlots.get(i);
                    int pokemonId = remainingPokemonIDs.get(i);
                    board[slot.y][slot.x] = pokemonId;
                }

                gameBoardView.invalidate(); // Vẽ lại bàn cờ
                updateUI();
            } else {
                Toast.makeText(this, "Bạn đã hết lượt đổi!", Toast.LENGTH_SHORT).show();
            }
        }
    
        private void handleWin() {
            // Dừng timer khi thắng
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            Toast.makeText(this, "Bạn đã thắng màn " + currentLevel + "!", Toast.LENGTH_LONG).show();
            currentLevel++;
            startNewGame();
        }
    
        private void updateUI() {
            tvLevel.setText(String.valueOf(currentLevel));
            tvScore.setText(String.valueOf(currentScore));
            tvShuffleCount.setText(String.valueOf(shufflesLeft));
            gameBoardView.invalidate(); // Rất quan trọng: Yêu cầu GameBoardView vẽ lại
        }
    
        // --- THUẬT TOÁN TÌM ĐƯỜNG ---
        private List<Point> findPath(Point p1, Point p2) {
            List<Point> path = new ArrayList<>();
            path.add(p1);
    
            // 1. Kiểm tra đường thẳng (I-path)
            if (checkLine(p1, p2)) {
                path.add(p2);
                return path;
            }
    
            // 2. Kiểm tra 1 rẽ (L-path)
            Point corner = checkLPath(p1, p2);
            if (corner != null) {
                path.add(corner);
                path.add(p2);
                return path;
            }
    
            // 3. Kiểm tra 2 rẽ (U/Z-path)
            List<Point> uPath = checkUPath(p1, p2);
            if (uPath != null) {
                path.addAll(uPath);
                return path;
            }
    
            return null; // Không tìm thấy đường
        }
    
        private boolean checkLine(Point p1, Point p2) {
            // Cùng hàng
            if (p1.y == p2.y) {
                int start = Math.min(p1.x, p2.x);
                int end = Math.max(p1.x, p2.x);
                for (int i = start + 1; i < end; i++) {
                    if (board[p1.y][i] != 0) return false;
                }
                return true;
            }
            // Cùng cột
            if (p1.x == p2.x) {
                int start = Math.min(p1.y, p2.y);
                int end = Math.max(p1.y, p2.y);
                for (int i = start + 1; i < end; i++) {
                    if (board[i][p1.x] != 0) return false;
                }
                return true;
            }
            return false;
        }
    
        private Point checkLPath(Point p1, Point p2) {
            // Góc 1: (p1.x, p2.y)
            Point c1 = new Point(p1.x, p2.y);
            if (board[c1.y][c1.x] == 0 && checkLine(p1, c1) && checkLine(c1, p2)) {
                return c1;
            }
            // Góc 2: (p2.x, p1.y)
            Point c2 = new Point(p2.x, p1.y);
            if (board[c2.y][c2.x] == 0 && checkLine(p1, c2) && checkLine(c2, p2)) {
                return c2;
            }
            return null;
        }
    
        private List<Point> checkUPath(Point p1, Point p2) {
            // Mở rộng từ p1
            for (int i = 0; i < board[0].length; i++) {
                Point testPoint = new Point(i, p1.y);
                if(board[testPoint.y][testPoint.x] == 0 || testPoint.equals(p2)) {
                    if (checkLine(p1, testPoint)) {
                        Point corner = checkLPath(testPoint, p2);
                        if(corner != null) {
                            List<Point> path = new ArrayList<>();
                            path.add(testPoint);
                            path.add(corner);
                            path.add(p2);
                            return path;
                        }
                    }
                }
            }
            // Mở rộng từ p2
            for (int i = 0; i < board.length; i++) {
                Point testPoint = new Point(p1.x, i);
                if(board[testPoint.y][testPoint.x] == 0 || testPoint.equals(p2)) {
                    if (checkLine(p1, testPoint)) {
                        Point corner = checkLPath(testPoint, p2);
                        if(corner != null) {
                            List<Point> path = new ArrayList<>();
                            path.add(testPoint);
                            path.add(corner);
                            path.add(p2);
                            return path;
                        }
                    }
                }
            }
            return null;
        }
    
        private boolean isMoveAvailable() {
            List<Point> remainingTiles = new ArrayList<>();
            for (int i = 1; i <= TOTAL_ROWS; i++) {
                for (int j = 1; j <= TOTAL_COLS; j++) {
                    if (board[i][j] != 0) {
                        remainingTiles.add(new Point(j, i));
                    }
                }
            }
    
            for (int i = 0; i < remainingTiles.size(); i++) {
                for (int j = i + 1; j < remainingTiles.size(); j++) {
                    Point p1 = remainingTiles.get(i);
                    Point p2 = remainingTiles.get(j);
                    if (board[p1.y][p1.x] == board[p2.y][p2.x]) {
                        if (findPath(p1, p2) != null) {
                            return true; // Tìm thấy ít nhất một cặp có thể nối
                        }
                    }
                }
            }
            return false; // Không còn nước đi
        }
    
        private void hideSystemUI() {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            // Cấu hình để ẩn các thanh hệ thống
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            // Cấu hình để các thanh tự động ẩn lại sau khi người dùng vuốt ra
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    
        // ================================================================
        // CÁC HÀM HELPER ĐỂ DI CHUYỂN BÀN CỜ
        // ================================================================
    
        /**
         * Dồn một cột lên trên, lấp đầy ô trống tại (row, col)
         */
        private void shiftColumnUp(int col) {
            List<Integer> remaining = new ArrayList<>();
            // Thu thập các ô còn lại
            for (int r = 1; r <= TOTAL_ROWS; r++) {
                if (board[r][col] != 0) {
                    remaining.add(board[r][col]);
                }
            }
            // Đặt lại từ trên xuống
            for (int r = 1; r <= TOTAL_ROWS; r++) {
                if (r <= remaining.size()) {
                    board[r][col] = remaining.get(r - 1);
                } else {
                    board[r][col] = 0;
                }
            }
        }
    
        /**
         * Dồn một cột xuống dưới, lấp đầy ô trống tại (row, col)
         */
        private void shiftColumnDown(int col) {
            List<Integer> remaining = new ArrayList<>();
            // Thu thập các ô còn lại
            for (int r = 1; r <= TOTAL_ROWS; r++) {
                if (board[r][col] != 0) {
                    remaining.add(board[r][col]);
                }
            }
            // Đặt lại từ dưới lên
            for (int r = TOTAL_ROWS; r >= 1; r--) {
                int index = r - (TOTAL_ROWS - remaining.size()) - 1;
                if (index >= 0) {
                    board[r][col] = remaining.get(index);
                } else {
                    board[r][col] = 0;
                }
            }
        }
    
        /**
         * Dồn một hàng sang trái, lấp đầy ô trống tại (row, col)
         */
        private void shiftRowLeft(int row) {
            List<Integer> remaining = new ArrayList<>();
            // Thu thập
            for (int c = 1; c <= TOTAL_COLS; c++) {
                if (board[row][c] != 0) {
                    remaining.add(board[row][c]);
                }
            }
            // Đặt lại
            for (int c = 1; c <= TOTAL_COLS; c++) {
                if (c <= remaining.size()) {
                    board[row][c] = remaining.get(c - 1);
                } else {
                    board[row][c] = 0;
                }
            }
        }
    
        /**
         * Dồn một hàng sang phải, lấp đầy ô trống tại (row, col)
         */
        private void shiftRowRight(int row) {
            List<Integer> remaining = new ArrayList<>();
            // Thu thập
            for (int c = 1; c <= TOTAL_COLS; c++) {
                if (board[row][c] != 0) {
                    remaining.add(board[row][c]);
                }
            }
            // Đặt lại
            for (int c = TOTAL_COLS; c >= 1; c--) {
                int index = c - (TOTAL_COLS - remaining.size()) - 1;
                if (index >= 0) {
                    board[row][c] = remaining.get(index);
                } else {
                    board[row][c] = 0;
                }
            }
        }
    
        /**
         * Dồn chéo từ dưới góc bất kỳ lên, lấp đầy ô trống tại (row, col)
         * Ưu tiên kéo từ dưới-trái
         */
        private void cascadeUp(int row, int col) {
            for (int r = row; r > 1; r--) {
                // Tìm ô để kéo lên
                Point source = null;
                if (col > 1 && board[r][col - 1] != 0) { // Ưu tiên dưới-trái
                    source = new Point(col - 1, r);
                } else if (col < TOTAL_COLS && board[r][col + 1] != 0) { // Nếu không có thì lấy dưới-phải
                    source = new Point(col + 1, r);
                }
    
                if (source != null) {
                    board[r - 1][col] = board[source.y][source.x];
                    board[source.y][source.x] = 0;
                } else {
                    board[r-1][col] = 0; // Nếu không có gì để kéo thì làm trống
                }
            }
        }
    
        /**
         * Dồn chéo từ trên góc bất kỳ xuống, lấp đầy ô trống tại (row, col)
         * Ưu tiên kéo từ trên-trái
         */
        private void cascadeDown(int row, int col) {
            for (int r = row; r < TOTAL_ROWS; r++) {
                // Tìm ô để kéo xuống
                Point source = null;
                if (col > 1 && board[r][col - 1] != 0) { // Ưu tiên trên-trái
                    source = new Point(col - 1, r);
                } else if (col < TOTAL_COLS && board[r][col + 1] != 0) { // Nếu không có thì lấy trên-phải
                    source = new Point(col + 1, r);
                }
    
                if (source != null) {
                    board[r + 1][col] = board[source.y][source.x];
                    board[source.y][source.x] = 0;
                } else {
                    board[r+1][col] = 0; // Nếu không có gì để kéo thì làm trống
                }
            }
        }

        /**
         * Hàm chính điều khiển việc di chuyển bàn cờ dựa trên màn chơi hiện tại.
         * PHIÊN BẢN ĐÃ SỬA LỖI CHO CÁC Ô LIỀN KỀ.
         * @param p1 Tọa độ ô đầu tiên bị xóa
         * @param p2 Tọa độ ô thứ hai bị xóa
         */
        private void shiftBoard(Point p1, Point p2) {
            final int centerX = TOTAL_COLS / 2;
            final int centerY = TOTAL_ROWS / 2;

            switch (currentLevel) {
                case 2: // Màn 2: Dồn từ dưới lên (Shift UP)
                    shiftColumnDown(p1.x);
                    if (p1.x != p2.x) shiftColumnDown(p2.x);
                    break;

                case 3: // Màn 3: Dồn từ trên xuống (Shift DOWN)
                    shiftColumnUp(p1.x);
                    if (p1.x != p2.x) shiftColumnUp(p2.x);
                    break;

                case 4: // Màn 4: Dồn từ phải qua trái (Shift LEFT)
                    shiftRowLeft(p1.y);
                    if (p1.y != p2.y) shiftRowLeft(p2.y);
                    break;

                case 5: // Màn 5: Dồn từ trái qua phải (Shift RIGHT)
                    shiftRowRight(p1.y);
                    if (p1.y != p2.y) shiftRowRight(p2.y);
                    break;

                case 6: // Màn 6: Chỉa ra 2 bên (từ tâm ra)
                    if (p1.x <= centerX) shiftRowLeft(p1.y);
                    else shiftRowRight(p1.y);
                    if (p1.y != p2.y) { // Nếu 2 ô khác hàng
                        if (p2.x <= centerX) shiftRowLeft(p2.y);
                        else shiftRowRight(p2.y);
                    }
                    break;

                case 7: // Màn 7: Dồn vào 2 bên (từ ngoài vào)
                    if (p1.x <= centerX) shiftRowRight(p1.y);
                    else shiftRowLeft(p1.y);
                    if (p1.y != p2.y) {
                        if (p2.x <= centerX) shiftRowRight(p2.y);
                        else shiftRowLeft(p2.y);
                    }
                    break;

                // THAY THẾ MÀN 8 VÀ 9
                case 8: // Màn 8: Dồn vào giữa theo chiều dọc
                    if(p1.y <= centerY) shiftColumnDown(p1.x);
                    else shiftColumnUp(p1.x);
                    if(p1.x != p2.x){
                        if(p2.y <= centerY) shiftColumnDown(p2.x);
                        else shiftColumnUp(p2.x);
                    }
                    break;

                case 9: // Màn 9: Dồn ra biên theo chiều dọc
                    if(p1.y <= centerY) shiftColumnUp(p1.x);
                    else shiftColumnDown(p1.x);
                    if(p1.x != p2.x){
                        if(p2.y <= centerY) shiftColumnUp(p2.x);
                        else shiftColumnDown(p2.x);
                    }
                    break;

                case 10: // Màn 10: Nửa phải dồn sang phải
                    if (p1.x > centerX) shiftRowRight(p1.y);
                    if (p2.x > centerX && p1.y != p2.y) shiftRowRight(p2.y);
                    break;

                case 11: // Màn 11: Nửa trái dồn sang trái
                    if (p1.x <= centerX) shiftRowLeft(p1.y);
                    if (p2.x <= centerX && p1.y != p2.y) shiftRowLeft(p2.y);
                    break;

                default:
                    // Màn 1 và các màn khác không làm gì cả
                    break;
            }
        }

        private void restartGameFromBeginning() {
            // Dừng timer hiện tại nếu có
            if (gameTimer != null) {
                gameTimer.cancel();
            }

            // Reset tất cả các chỉ số về trạng thái ban đầu
            currentLevel = 1;
            currentScore = 0;
            shufflesLeft = INITIAL_SHUFFLES;

            // Bắt đầu màn 1
            startNewGame();
        }

        @Override
        protected void onPause() {
            super.onPause();
            if (backgroundMusicPlayer != null && backgroundMusicPlayer.isPlaying()) {
                backgroundMusicPlayer.pause();
            }
        }
    
        @Override
        protected void onResume() {
            super.onResume();
            if (backgroundMusicPlayer != null && !backgroundMusicPlayer.isPlaying() && !isMuted) {
                backgroundMusicPlayer.start();
            }
        }
    
        @Override
        protected void onDestroy() {
            super.onDestroy();
            // Dừng timer để tránh memory leak
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            if (backgroundMusicPlayer != null) {
                backgroundMusicPlayer.stop();
                backgroundMusicPlayer.release();
                backgroundMusicPlayer = null;
            }
        }
    }