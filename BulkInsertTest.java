import java.sql.*;
import java.util.Random;

public class BulkInsertTest {
    public static void main(String[] args) {
        // 数据库连接配置
        String dbName = "postgres";
        String dbUser = "gaussdb";
        String dbPassword = "Secretpassword@123";
        String dbHost = "localhost";
        String dbPort = "15433";
        String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

        // 设置插入数据量
        int totalRows = 1000000;

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {

            // 删除旧表并重新创建表
            String dropTableSQL = "DROP TABLE IF EXISTS bulk_insert_test";
            String createTableSQL = "CREATE TABLE bulk_insert_test (" +
                    "id SERIAL PRIMARY KEY, " +
                    "data TEXT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(dropTableSQL);  // 删除旧表
                stmt.executeUpdate(createTableSQL); // 创建新表
            }

            long startTime = System.currentTimeMillis();
            conn.setAutoCommit(false);

            // 插入数据
            String insertSQL = "INSERT INTO bulk_insert_test (data) VALUES (?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                Random random = new Random();
                int batchSize = 1000;

                for (int i = 0; i < totalRows; i++) {
                    pstmt.setString(1, "random_data_" + random.nextInt(totalRows));
                    pstmt.addBatch();

                    if (i % batchSize == 0) {
                        pstmt.executeBatch();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                e.printStackTrace();
                conn.rollback();
            }

            long endTime = System.currentTimeMillis();
            System.out.println("插入 " + totalRows + " 行数据用时：" + (endTime - startTime) / 1000.0 + " 秒");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}