package com.example.gamesolver;

public class Solver {
    private int[][] grid;
    private int[][] originalGrid;
    // N is the size of the 2D matrix N*N
    private int game;
    private static int N;

    private static final int SUDOKU = 0;
    private static final int MAGIC_SQUARE=1;

    private static final int MAXVAL = 9;
    private static final int SUM = 15;

    public Solver(int game){
        this.game=game;
        switch(game){
            case SUDOKU:
                N = 9;
                break;
            case MAGIC_SQUARE:
                N = 3;
                break;
        }
    }
    private void setMatrixFromArray(int[] initArray) {
        this.grid=new int[N][N];
        this.originalGrid = new int [N][N];
        int index=0;
        for(int i=0;i<N;i++){
            for(int j=0;j<N;j++){
             grid[i][j]=initArray[index];
             originalGrid[i][j]=initArray[index];
             index++;
            }
        }
    }

    //Sudoku Solver
    private int sum(){
        int s =0;
        for(int i=0;i<N;i++){
            for(int j=0;j<N;j++){
                s+=grid[i][j];
            }
        }
        return s;
    }

    public int[][] getMatrix() {
        return grid;
    }

    /* Takes a partially filled-in grid and attempts
            to assign values to all unassigned locations in
            such a way to meet the requirements for
            Sudoku solution (non-duplication across rows,
            columns, and boxes) */
    public int[][] solve(int[] inputGrid){
        setMatrixFromArray(inputGrid);
        if (game ==SUDOKU) {
            if (sum() == 0) {
                return null;
            } else if (solveSudoku(grid, 0, 0)) {
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        if (grid[i][j] == originalGrid[i][j])
                            grid[i][j] = 0;
                    }
                }
                return grid;
            } else {
                return null;
            }
        }
        else {
            boolean solved = solveMagicSquare(grid);
            if (!solved) {
                return null;
            }
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (grid[i][j] == originalGrid[i][j]) {
                        grid[i][j] = 0;
                    }
                }
            }
            return grid;
        }
    }

    private static boolean solveSudoku(int[][] grid, int row, int col) {
        /*if we have reached the 8th
		row and 9th column (0
		indexed matrix) ,
		we are returning true to avoid further
		backtracking	 */
        if (row == N - 1 && col == N)
            return true;

        // Check if column value becomes 9 ,
        // we move to next row
        // and column start from 0
        if (col == N) {
            row++;
            col = 0;
        }

        // Check if the current position
        // of the grid already
        // contains value >0, we iterate
        // for next column
        if (grid[row][col] != 0)
            return solveSudoku(grid, row, col + 1);

        for (int num = 1; num < 10; num++) {

            // Check if it is safe to place
            // the num (1-9) in the
            // given row ,col ->we move to next column
            if (isSafe(grid, row, col, num)) {

            /* assigning the num in the current
            (row,col) position of the grid and
            assuming our assigned num in the position
            is correct */
                grid[row][col] = num;

                // Checking for next
                // possibility with next column
                if (solveSudoku(grid, row, col + 1))
                    return true;
            }
        /* removing the assigned num , since our
        assumption was wrong , and we go for next
        assumption with diff num value */
            grid[row][col] = 0;
        }
        return false;
    }

    // Check whether it will be legal
    // to assign num to the
    // given row, col
    static boolean isSafe(int[][] grid, int row, int col,
                          int num)
    {

        // Check if we find the same num
        // in the similar row , we
        // return false
        for (int x = 0; x <= 8; x++)
            if (grid[row][x] == num)
                return false;

        // Check if we find the same num
        // in the similar column ,
        // we return false
        for (int x = 0; x <= 8; x++)
            if (grid[x][col] == num)
                return false;

        // Check if we find the same num
        // in the particular 3*3
        // matrix, we return false
        int startRow = row - row % 3, startCol
                = col - col % 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (grid[i + startRow][j + startCol] == num)
                    return false;

        return true;
    }

    /*-------------End Sudoku Solver------------------*/


    /*-------------Magic Square Solver----------------*/
    private boolean checkGrid(int[][] grid) {
        int row, col;
        // check if there is a value not insert
        for (row = 0; row < N; row++) {
            for (col = 0; col < N; col++) {
                if (grid[row][col] == 0)
                    return false;
            }
        }

        int sd1 = 0;
        int sd2 = 0;
        int srows;
        int scols;
        // check if the sum of values in each row or cols is equal to SUM
        for (row = 0; row < N; row++) {
            srows = 0;
            scols = 0;

            sd1 = sd1 + grid[row][row]; // diagonal top left - bottom right
            sd2 = sd2 + grid[row][N - 1 - row]; // diagonal top right - bottom left
            for (col = 0; col < N; col++) {
                srows = srows + grid[row][col]; // fisso la riga e vario la colonna
                scols = scols + grid[col][row]; // fisso la colonna e vario la riga
            }

            if (srows != SUM || scols != SUM)
                return false;
        }

        // check diagonal sums are equal to SUM
        if (sd1 != SUM || sd2 != SUM)
            return false;

        // We have a magic square!
        return true;
    }

    // A backtracking/recursive function to check all possible combinations of
    // numbers until a solution is found
    private boolean solveMagicSquare(int[][] grid) {
        // Find next empty cell
        int row = 0, col = 0;
        boolean condition;
        for (int i = 0; i < MAXVAL; i++) {
            row = i / N;
            col = i % N;
            if (grid[row][col] == 0) {
                for (int value = 1; value < MAXVAL + 1; value++) { // MAXVAL+1 per includere il valore massimo
                    // Can only use numbers that have not been used yet
                    condition = false;
                    for (int j = 0; j < N; j++) {
                        if (in(value, grid[j])) {
                            condition = true;
                            break;
                        }
                    }
                    if (!condition) {
                        grid[row][col] = value;

                        if (checkGrid(grid)) {
                            // print("Grid Complete and Checked")
                            return true;
                        } else if (solveMagicSquare(grid)) {
                            return true;
                        }
                    }
                }

                break;
            }
        }
        //Backtrack
        grid[row][col] = 0;
        return false;
    }

    private boolean in(int el, int[] row) {
        for (int i = 0; i < row.length; i++) {
            if (row[i] == el)
                return true;
        }
        return false;
    }

    /*-----------End Magic Square Solver--------------*/

    /* A utility function to print grid */
    static String print(int[][] grid)
    {
        String result="";
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print(grid[i][j] + " ");
                result+=grid[i][j] + " ";
            }
            result+="\n";
            System.out.println();
        }
        return result;
    }
}