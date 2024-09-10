package hao1337.lib;

public class Matrix {
    public static int[][] rotate(int[][] matrix, int rotation) {
        int n = matrix.length;
        int[][] rotatedMatrix = new int[n][n];

        switch (rotation) {
            case 0:
                rotatedMatrix = matrix;
                break;
            case 1:
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        rotatedMatrix[j][n - 1 - i] = matrix[i][j];
                    }
                }
                break;
            case 2:
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        rotatedMatrix[n - 1 - i][n - 1 - j] = matrix[i][j];
                    }
                }
                break;
            case 3:
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        rotatedMatrix[n - 1 - j][i] = matrix[i][j];
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid rotation value. Must be 0, 1, 2, or 3.");
        }

        return rotatedMatrix;
    }

    public static void squareFill(int[][] matrix, int startRow, int startCol, int size, int value) {
        // if (size == 1) return;
        int endRow = Math.min(startRow + size, matrix.length);
        int endCol = Math.min(startCol + size, matrix[0].length);

        for (int i = startRow; i < endRow; i++) {
            for (int j = startCol; j < endCol; j++) {
                matrix[i][j] = value;
            }
        }
    }

    public static void fill(int[][] array, int value) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                array[i][j] = value;
            }
        }
    }
}