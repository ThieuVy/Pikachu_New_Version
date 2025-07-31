package com.example.app1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.app.Dialog;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    // Chế độ Debug: Thay đổi số này để nhảy đến màn muốn test
    private static final int DEBUG_START_LEVEL = 15;

    // --- Cấu hình Game ---
    private static final int TOTAL_ROWS = 9;
    private static final int TOTAL_COLS = 16;
    private static final int TOTAL_POKEMON_TYPES = 18;
    private static final int GAME_TIME_IN_SECONDS = 260;
    private static final int INITIAL_SHUFFLES = 10;

    // --- Các thành phần UI và Trạng thái Game (giữ nguyên) ---
    private TextView tvLevel, tvShuffleCount, tvScore;
    private ImageButton btnMute, btnShuffle, btnReplay;
    private ProgressBar timeProgressBar;
    private GameBoardView gameBoardView;

    private int[][] board;
    private Bitmap[] pokemonImages;
    private int currentLevel = DEBUG_START_LEVEL;
    private int currentScore = 0;
    private int shufflesLeft = INITIAL_SHUFFLES;
    private Point firstSelection = null;
    private int remainingPairs;

    private boolean isMuted = false;
    private MediaPlayer backgroundMusicPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CountDownTimer gameTimer;


    // ... (Toàn bộ các hàm từ onCreate đến trước handleTileClick giữ nguyên) ...
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
                // Cập nhật thanh thời gian
                int progress = (int) (millisUntilFinished * 100 / (GAME_TIME_IN_SECONDS * 1000));
                timeProgressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                // Hết giờ!
                timeProgressBar.setProgress(0);

                // GỌI HÀM HIỂN THỊ DIALOG TẠI ĐÂY
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

        // Gán layout tùy chỉnh đã tạo ở bước 1 vào Dialog
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
            restartGameFromBeginning(); // Gọi hàm chơi lại từ màn đầu
        });

        // Gán sự kiện cho nút "Thoát Game"
        btnExit.setOnClickListener(v -> {
            dialog.dismiss(); // Đóng dialog trước
            finish(); // Đóng Activity hiện tại và thoát game
        });

        // Không cho phép hủy dialog bằng nút back hoặc chạm ra ngoài
        dialog.setCancelable(false);

        // Hiển thị dialog
        dialog.show();
    }

    private void handleTileClick(int row, int col) {
        if (board[row][col] == 0) return;

        if (firstSelection == null) {
            firstSelection = new Point(col, row);
            gameBoardView.setSelectedTile(firstSelection);
        } else {
            Point secondSelection = new Point(col, row);
            if (firstSelection.equals(secondSelection)) {
                firstSelection = null;
                gameBoardView.setSelectedTile(null);
                return;
            }

            if (board[firstSelection.y][firstSelection.x] == board[secondSelection.y][secondSelection.x]) {
                List<Point> path = findPath(firstSelection, secondSelection);
                if (path != null) {
                    final Point finalFirstSelection = firstSelection;
                    final Point finalSecondSelection = secondSelection;
                    firstSelection = null;
                    gameBoardView.setEnabled(false);
                    gameBoardView.drawPath(path);

                    handler.postDelayed(() -> {
                        board[finalFirstSelection.y][finalFirstSelection.x] = 0;
                        board[finalSecondSelection.y][finalSecondSelection.x] = 0;

                        // =======================================================
                        // THAY ĐỔI: Thêm logic cho màn 15 (cuộn lên)
                        // =======================================================
                        if (currentLevel == 14) {
                            shiftAllRowsDown();
                        } else if (currentLevel == 15) { // Màn mới
                            shiftAllRowsUp();
                        } else {
                            shiftBoard(finalFirstSelection, finalSecondSelection);
                        }

                        gameBoardView.clearPathAndSelection();
                        currentScore += 10;
                        remainingPairs--;
                        updateUI();
                        checkGameState();
                        gameBoardView.setEnabled(true);

                    }, 300);
                } else {
                    firstSelection = null;
                    gameBoardView.setSelectedTile(null);
                }
            } else {
                firstSelection = null;
                gameBoardView.setSelectedTile(null);
            }
        }
    }

    private void checkGameState() {
        if (remainingPairs == 0) {
            handleWin();
        } else if (!isMoveAvailable()) {
            Toast.makeText(this, "Không còn nước đi! Tự động xáo trộn.", Toast.LENGTH_SHORT).show();
            handleShuffle();
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
    // =================================================================================
    // CÁC HÀM DI CHUYỂN BÀN CỜ
    // =================================================================================

    private void shiftRowLeft(int row, int startCol, int endCol) {
        List<Integer> remaining = new ArrayList<>();
        for (int c = startCol; c <= endCol; c++) {
            if (board[row][c] != 0)
                remaining.add(board[row][c]);
        }
        int currentIndex = 0;
        for (int c = startCol; c <= endCol; c++) {
            board[row][c] = (currentIndex < remaining.size()) ? remaining.get(currentIndex++) : 0;
        }
    }

    private void shiftRowRight(int row, int startCol, int endCol) {
        List<Integer> remaining = new ArrayList<>();
        for (int c = startCol; c <= endCol; c++) {
            if (board[row][c] != 0)
                remaining.add(board[row][c]);
        }
        int currentIndex = remaining.size() - 1;
        for (int c = endCol; c >= startCol; c--) {
            board[row][c] = (currentIndex >= 0) ? remaining.get(currentIndex--) : 0;
        }
    }

    private void shiftColumnUp(int col, int startRow, int endRow) {
        List<Integer> remaining = new ArrayList<>();
        for (int r = startRow; r <= endRow; r++) {
            if (board[r][col] != 0)
                remaining.add(board[r][col]);
        }
        int currentIndex = 0;
        for (int r = startRow; r <= endRow; r++) {
            board[r][col] = (currentIndex < remaining.size()) ? remaining.get(currentIndex++) : 0;
        }
    }

    private void shiftColumnDown(int col, int startRow, int endRow) {
        List<Integer> remaining = new ArrayList<>();
        for (int r = startRow; r <= endRow; r++) {
            if (board[r][col] != 0)
                remaining.add(board[r][col]);
        }
        int currentIndex = remaining.size() - 1;
        for (int r = endRow; r >= startRow; r--) {
            board[r][col] = (currentIndex >= 0) ? remaining.get(currentIndex--) : 0;
        }
    }

    /**
     * Màn 14: Cuộn TOÀN BỘ bàn cờ xuống 1 hàng.
     */
    private void shiftAllRowsDown() {
        int[] lastRow = board[TOTAL_ROWS];
        for (int r = TOTAL_ROWS; r > 1; r--) {
            board[r] = board[r - 1];
        }
        board[1] = lastRow;
    }

    /**
     * THÊM HÀM MỚI
     * Màn 15: Cuộn TOÀN BỘ bàn cờ lên 1 hàng.
     */
    private void shiftAllRowsUp() {
        // Lưu lại hàng đầu tiên (hàng 1)
        int[] firstRow = board[1];
        // Dịch chuyển các hàng từ 2->1, 3->2, ..., 9->8
        for (int r = 1; r < TOTAL_ROWS; r++) {
            board[r] = board[r + 1];
        }
        // Đưa hàng đầu đã lưu xuống cuối (thành hàng 9)
        board[TOTAL_ROWS] = firstRow;
    }

    private void cascadeFill(int row, int col) {
        if (row > 1 && col > 1 && board[row - 1][col - 1] != 0) {
            board[row][col] = board[row - 1][col - 1];
            board[row - 1][col - 1] = 0;
            cascadeFill(row - 1, col - 1);
        } else if (row > 1 && board[row - 1][col] != 0) {
            shiftColumnUp(col, 1, row);
        }
    }

    /**
     * Màn 17: Đổ Nghiêng Trái (↙)
     * Các ô trượt theo đường chéo xuống dưới bên trái.
     * Ưu tiên: Chéo > Dọc > Ngang.
     */
    private void cascadeFillFallLeft() {
        boolean tileMoved;
        do {
            tileMoved = false;
            // Quét từ trên xuống dưới, trái qua phải để lấp đầy chỗ trống
            for (int r = 1; r <= TOTAL_ROWS; r++) {
                for (int c = 1; c <= TOTAL_COLS; c++) {
                    if (board[r][c] == 0) {
                        // Ưu tiên 1: Trượt chéo từ trên-phải (↖)
                        if (r > 1 && c < TOTAL_COLS && board[r - 1][c + 1] != 0) {
                            board[r][c] = board[r - 1][c + 1];
                            board[r - 1][c + 1] = 0;
                            tileMoved = true;
                        }
                        // Ưu tiên 2: Trượt dọc từ trên xuống
                        else if (r > 1 && board[r - 1][c] != 0) {
                            board[r][c] = board[r - 1][c];
                            board[r - 1][c] = 0;
                            tileMoved = true;
                        }
                        // Ưu tiên 3: Trượt ngang từ phải qua trái
                        else if (c < TOTAL_COLS && board[r][c + 1] != 0) {
                            board[r][c] = board[r][c+1];
                            board[r][c+1] = 0;
                            tileMoved = true;
                        }
                    }
                }
            }
        } while (tileMoved); // Lặp lại cho đến khi không còn ô nào có thể di chuyển
    }

    /**
     * Màn 18: Nghiêng Ngược Phải (↗)
     * Các ô trượt theo đường chéo lên trên bên phải.
     * Ưu tiên: Chéo > Ngang > Dọc.
     */
    private void cascadeFillRiseRight() {
        boolean tileMoved;
        do {
            tileMoved = false;
            // Quét từ dưới lên trên, phải qua trái
            for (int r = TOTAL_ROWS; r >= 1; r--) {
                for (int c = TOTAL_COLS; c >= 1; c--) {
                    if (board[r][c] == 0) {
                        // Ưu tiên 1: Trượt chéo từ dưới-trái (↙)
                        if (r < TOTAL_ROWS && c > 1 && board[r + 1][c - 1] != 0) {
                            board[r][c] = board[r + 1][c - 1];
                            board[r + 1][c - 1] = 0;
                            tileMoved = true;
                        }
                        // Ưu tiên 2: Trượt ngang từ trái qua phải
                        else if (c > 1 && board[r][c - 1] != 0) {
                            board[r][c] = board[r][c - 1];
                            board[r][c - 1] = 0;
                            tileMoved = true;
                        }
                        // Ưu tiên 3: Trượt dọc từ dưới lên
                        else if (r < TOTAL_ROWS && board[r + 1][c] != 0) {
                            board[r][c] = board[r + 1][c];
                            board[r + 1][c] = 0;
                            tileMoved = true;
                        }
                    }
                }
            }
        } while (tileMoved);
    }

    /**
     * Màn 19: Nghiêng Ngược Trái (↖)
     * Các ô trượt theo đường chéo lên trên bên trái.
     * Ưu tiên: Chéo > Dọc > Ngang.
     */
    private void cascadeFillRiseLeft() {
        boolean tileMoved;
        do {
            tileMoved = false;
            // Quét từ dưới lên trên, trái qua phải
            for (int r = TOTAL_ROWS; r >= 1; r--) {
                for (int c = 1; c <= TOTAL_COLS; c++) {
                    if (board[r][c] == 0) {
                        // Ưu tiên 1: Trượt chéo từ dưới-phải (↘)
                        if (r < TOTAL_ROWS && c < TOTAL_COLS && board[r + 1][c + 1] != 0) {
                            board[r][c] = board[r + 1][c + 1];
                            board[r + 1][c + 1] = 0;
                            tileMoved = true;
                        }
                        // Ưu tiên 2: Trượt dọc từ dưới lên
                        else if (r < TOTAL_ROWS && board[r + 1][c] != 0) {
                            board[r][c] = board[r + 1][c];
                            board[r + 1][c] = 0;
                            tileMoved = true;
                        }
                        // Ưu tiên 3: Trượt ngang từ phải qua trái
                        else if (c < TOTAL_COLS && board[r][c + 1] != 0) {
                            board[r][c] = board[r][c + 1];
                            board[r][c + 1] = 0;
                            tileMoved = true;
                        }
                    }
                }
            }
        } while (tileMoved);
    }

    private void shiftBoard(Point p1, Point p2) {
        // ===============================================================
        // THAY ĐỔI: Cập nhật logic cho các màn mới
        // ===============================================================
        if (currentLevel == 16) {
            if (p1.y > p2.y || (p1.y == p2.y && p1.x > p2.x)) {
                Point temp = p1;
                p1 = p2;
                p2 = temp;
            }
            cascadeFill(p1.y, p1.x);
            cascadeFill(p2.y, p2.x);
        } else if (currentLevel == 17) {
            cascadeFillFallLeft();
        } else if (currentLevel == 18) {
            cascadeFillRiseRight();
        } else if (currentLevel == 19) {
            cascadeFillRiseLeft();
        }
        else {
            // Giữ lại logic xử lý cho các màn cũ
            processShiftForPoint(p1);
            processShiftForPoint(p2);
        }
    }

    private void processShiftForPoint(Point p) {
        final int horizontalMidpoint = 4;
        final int verticalMidpoint = 8;

        switch (currentLevel) {
            case 2:
                shiftColumnUp(p.x, 1, TOTAL_ROWS);
                break;
            case 3:
                shiftColumnDown(p.x, 1, TOTAL_ROWS);
                break;
            case 4:
                shiftRowLeft(p.y, 1, TOTAL_COLS);
                break;
            case 5:
                shiftRowRight(p.y, 1, TOTAL_COLS);
                break;
            case 6:
                if (p.x <= verticalMidpoint)
                    shiftRowLeft(p.y, 1, verticalMidpoint);
                else
                    shiftRowRight(p.y, verticalMidpoint + 1, TOTAL_COLS);
                break;
            case 7:
                if (p.x <= verticalMidpoint)
                    shiftRowRight(p.y, 1, verticalMidpoint);
                else
                    shiftRowLeft(p.y, verticalMidpoint + 1, TOTAL_COLS);
                break;
            case 8:
                if (p.y <= horizontalMidpoint)
                    shiftColumnUp(p.x, 1, horizontalMidpoint);
                else
                    shiftColumnDown(p.x, horizontalMidpoint + 1, TOTAL_ROWS);
                break;
            case 9:
                if (p.y <= horizontalMidpoint)
                    shiftColumnDown(p.x, 1, horizontalMidpoint);
                else
                    shiftColumnUp(p.x, horizontalMidpoint + 1, TOTAL_ROWS);
                break;
            case 10:
                if (p.x <= verticalMidpoint)
                    shiftRowLeft(p.y, 1, verticalMidpoint);
                break;
            case 11:
                if (p.x > verticalMidpoint)
                    shiftRowRight(p.y, verticalMidpoint + 1, TOTAL_COLS);
                break;
            case 12:
                if (p.y <= horizontalMidpoint) {
                    shiftColumnDown(p.x, 1, horizontalMidpoint);
                } else if (p.y > horizontalMidpoint + 1) {
                    shiftColumnUp(p.x, horizontalMidpoint + 2, TOTAL_ROWS);
                }
                break;
            case 13:
                if (p.y <= horizontalMidpoint) {
                    if (p.x <= verticalMidpoint) {
                        shiftColumnDown(p.x, 1, horizontalMidpoint);
                        shiftRowRight(p.y, 1, verticalMidpoint);
                    } else {
                        shiftColumnDown(p.x, 1, horizontalMidpoint);
                        shiftRowLeft(p.y, verticalMidpoint + 1, TOTAL_COLS);
                    }
                } else {
                    if (p.x <= verticalMidpoint) {
                        shiftColumnUp(p.x, horizontalMidpoint + 1, TOTAL_ROWS);
                        shiftRowRight(p.y, 1, verticalMidpoint);
                    } else {
                        shiftColumnUp(p.x, horizontalMidpoint + 1, TOTAL_ROWS);
                        shiftRowLeft(p.y, verticalMidpoint + 1, TOTAL_COLS);
                    }
                }
                break;

            default:
                break;
        }
    }


    private void restartGameFromBeginning() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        currentLevel = DEBUG_START_LEVEL;
        currentScore = 0;
        shufflesLeft = INITIAL_SHUFFLES;
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