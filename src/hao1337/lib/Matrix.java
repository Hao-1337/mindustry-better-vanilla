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
}