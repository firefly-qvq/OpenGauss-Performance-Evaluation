**Opengauss benchmarking report**



**Contents**

**Part I :key metrics**

**Part II :experiment**

​	**1.Basic simple select proformance **

​	**2.Large join proformance**

​	**3.aggregate function performance  **

​	**4.Data importation speed from large csv or other form using COPY command**

​	**5.Direct bulk insert porformance**

​	**6.Direct bulk update**

​	**7.Direct bulk delete**

​	**8.Index proformance**

​	**9.Transaction per second porformance(TPS) when the size of data increases(One of the scalar test)**

​	**10.Transaction per second proformance(TPS), under High-Concurrency Scenario**

​	**11.Latency of transaction**

​	**12.Try to optimize OpenGauss's proformance**

**Part III. Detailed Evaluation of OpenGauss**

**Part IV: Summary**



​			

**Part I :key metrics**

1.Basic query proformance

​	Execution time of basic query like insert, create, delete, select, update, join, aggregate function and so on.Especially the proformance of them when data size is large.

2.Bulk Data Operations Proformance 

​	Bulk insert or load large datasets (e.g., CSV or JSON).

​	Measure performance of batch updates and deletes.

3.Index proformance

​	How much improvement can index make to the database system.

​	How much time it will take to create index.

4.Query optimizer performance

​	Query optimizer is like the brain of a database system.It can optimize our query(choose the best way to compelete our task) .

​	We will test the query optimizer proformance by checking the query plan of the basic operations.

5.Overall performance under a scalar view

​	Can the database still preform well when the size of data and user increase.

​	What about the avg latency of the transaction?

​	This can be evaluated by a scalar test.

​	When a company has a large number of user, it's database system must be able to handle a large amount of user's queries at the same time.(高并发性) 



**Part II :experiment**

**1.Basic simple select proformance **

​	(1)design of the experiment

​		use the following command to proform simple select.(without index)

```sql
select * from title_basics where originaltitle like 'Titanic';
```

​		Record execution time.

​	(2)result

| Time | OpenGauss execution time(ms) | Postgres execution time(ms) |
| ---- | ---------------------------- | --------------------------- |
| 1    | 880                          | 486                         |
| 2    | 873                          | 459                         |
| 3    | 857                          | 463                         |
| 4    | 932                          | 481                         |
| 5    | 880                          | 463                         |

​		Postgres proforms better than opengauss.

**2.Large join proformance**

​	(1)design of the experiment 

​		We are going to create some tables and preform a very large join.

```sql
create table join_table_large_1(id int, num1 int);
create table join_table_large_2(id int, num2 int);
create table join_table_large_3(id int, num3 int);
create table join_table_large_4(id int, num4 int);
create table join_table_large_5(id int, num5 int);
create table join_table_large_6(id int, num6 int);

select * from join_table_large_1 inner join join_table_large_2 on join_table_large_1.id=join_table_large_2.id inner join join_table_large_3 on join_table_large_1.id=join_table_large_3.id inner join join_table_large_4 on join_table_large_3.id=join_table_large_4.id inner join join_table_large_5 on 1=1 inner join join_table_large_6 on join_table_large_5.id=join_table_large_6.id where join_table_large_3.num3 <10 and join_table_large_5.num5>90;
```

​	(2)result

| Time | OpenGauss execution time(ms) | Postgres execution time(ms) |
| ---- | ---------------------------- | --------------------------- |
| 1    | 6                            | 4                           |
| 2    | 6                            | 3                           |
| 3    | 5                            | 3                           |
| 4    | 4                            | 4                           |
| 5    | 5                            | 3                           |

​		They have nearly equal proformance here.

**3.aggregate function performance  **

​	(i).count() function

​		(1)design of experiment

​			We use a large dataset(size: approximately 2 GB), and use count() function to count how many rows does it has.	

​			Repeat 10 times, record execution time.

```sql
select count(*) from <table_name>;
```

​			Then use EXPLAIN ANALYZE to show the difference of their query plan.

```
EXPLAIN ANALYZE select count(*) from <table_name>;
```

​		(2)result of experiment

​			execution time:	

| Time | Postgres execution time(ms) | Opengauss execution time(s/ms) |
| ---- | --------------------------- | ------------------------------ |
| 1    | 466                         | 3419                           |
| 2    | 460                         | 2792                           |
| 3    | 455                         | 3672                           |
| 4    | 469                         | 2995                           |
| 5    | 457                         | 2227                           |
| 6    | 471                         | 2701                           |
| 7    | 429                         | 2193                           |
| 8    | 466                         | 2372                           |
| 9    | 484                         | 2499                           |
| 10   | 458                         | 2281                           |
| Avg  | 461.5                       | 2715.1                         |

​			Query plan:

​				For Postgres:

```
Finalize Aggregate  (cost=442310.70..442310.71 rows=1 width=8) (actual time=864.407..867.949 rows=1 loops=1)
  ->  Gather  (cost=442310.49..442310.70 rows=2 width=8) (actual time=864.072..867.924 rows=3 loops=1)
        Workers Planned: 2
        Workers Launched: 2
        ->  Partial Aggregate  (cost=441310.49..441310.50 rows=1 width=8) (actual time=823.443..823.443 rows=1 loops=3)
              ->  Parallel Seq Scan on synthetic_fraud_data  (cost=0.00..433515.59 rows=3117959 width=0) (actual time=0.182..736.915 rows=2494589 loops=3)
Planning Time: 0.329 ms
JIT:
  Functions: 8
"  Options: Inlining false, Optimization false, Expressions true, Deforming true"
"  Timing: Generation 2.257 ms, Inlining 0.000 ms, Optimization 1.104 ms, Emission 11.606 ms, Total 14.967 ms"
Execution Time: 869.654 ms

```

​				For OpenGauss:

```
Aggregate  (cost=499381.25..499381.26 rows=1 width=8) (actual time=3907.473..3907.473 rows=1 loops=1)
  ->  Seq Scan on synthetic_fraud_data  (cost=0.00..480672.40 rows=7483540 width=0) (actual time=0.685..3413.554 rows=7483766 loops=1)
Total runtime: 3907.837 ms

```

​	(ii).sum() and avg() function

​		(1)design of the experiment

​			use a large dataset(size:approximately:2GB).

```sql
select sum(<column_name>) from <table_name>;
select avg(<column_name>) from <table_name>;
```

```sql
select sum(amount) from synthetic_fraud_data;
select avg(amount) from synthetic_fraud_data;
```

​			Run the sql above in OpenGauss and Postgres 10 times each and record execution time.

​			Then use EXPLAIN ANALYZE to show the difference of their query plan.

```sql
explain analyze select sum(<column_name>) from <table_name>;
explain analyze select avg(<column_name>) from <table_name>;
```

```sql
explain analyze select sum(amount) from synthetic_fraud_data;
explain analyze select avg(amount) from synthetic_fraud_data;
```

​		(2)result

​			sum() performance:

| Time | Postgres execution time(ms) | OpenGauss execution time(ms) |
| ---- | --------------------------- | ---------------------------- |
| 1    | 705                         | 3217                         |
| 2    | 697                         | 2289                         |
| 3    | 706                         | 2287                         |
| 4    | 692                         | 2486                         |
| 5    | 846                         | 2361                         |
| 6    | 752                         | 2268                         |
| 7    | 694                         | 2439                         |
| 8    | 695                         | 2299                         |
| 9    | 705                         | 2284                         |
| 10   | 707                         | 2280                         |
| Avg  | 719.9                       | 2421                         |

​			Postgres query plan:

```
Finalize Aggregate  (cost=442310.70..442310.71 rows=1 width=8) (actual time=917.584..920.612 rows=1 loops=1)
  ->  Gather  (cost=442310.49..442310.70 rows=2 width=8) (actual time=917.247..920.592 rows=3 loops=1)
        Workers Planned: 2
        Workers Launched: 2
        ->  Partial Aggregate  (cost=441310.49..441310.50 rows=1 width=8) (actual time=903.604..903.605 rows=1 loops=3)
              ->  Parallel Seq Scan on synthetic_fraud_data  (cost=0.00..433515.59 rows=3117959 width=8) (actual time=0.065..730.288 rows=2494589 loops=3)
Planning Time: 0.404 ms
JIT:
  Functions: 11
"  Options: Inlining false, Optimization false, Expressions true, Deforming true"
"  Timing: Generation 1.094 ms, Inlining 0.000 ms, Optimization 0.847 ms, Emission 11.820 ms, Total 13.762 ms"
Execution Time: 958.495 ms

```

​			OpenGauss query plan:			

```
Aggregate  (cost=499399.51..499399.52 rows=1 width=16) (actual time=4400.217..4400.217 rows=1 loops=1)
  ->  Seq Scan on synthetic_fraud_data  (cost=0.00..480687.01 rows=7485001 width=8) (actual time=0.972..3542.462 rows=7483766 loops=1)
Total runtime: 4400.503 ms

```

​			avg() performance:

| Time | Postgres execution time(ms) | OpenGauss execution time(ms) |
| ---- | --------------------------- | ---------------------------- |
| 1    | 855                         | 3599                         |
| 2    | 860                         | 3629                         |
| 3    | 861                         | 3636                         |
| 4    | 862                         | 2921                         |
| 5    | 780                         | 3470                         |
| 6    | 782                         | 2872                         |
| 7    | 824                         | 2800                         |
| 8    | 788                         | 2838                         |
| 9    | 797                         | 2781                         |
| 10   | 791                         | 2944                         |
| Avg  | 820                         | 3149                         |

​			Postgres query plan:

```
Finalize Aggregate  (cost=442310.70..442310.71 rows=1 width=8) (actual time=1073.244..1075.595 rows=1 loops=1)
  ->  Gather  (cost=442310.49..442310.70 rows=2 width=32) (actual time=1073.072..1075.564 rows=3 loops=1)
        Workers Planned: 2
        Workers Launched: 2
        ->  Partial Aggregate  (cost=441310.49..441310.50 rows=1 width=32) (actual time=1041.467..1041.467 rows=1 loops=3)
              ->  Parallel Seq Scan on synthetic_fraud_data  (cost=0.00..433515.59 rows=3117959 width=8) (actual time=0.086..846.523 rows=2494589 loops=3)
Planning Time: 1.094 ms
JIT:
  Functions: 11
"  Options: Inlining false, Optimization false, Expressions true, Deforming true"
"  Timing: Generation 4.205 ms, Inlining 0.000 ms, Optimization 3.161 ms, Emission 11.699 ms, Total 19.066 ms"
Execution Time: 1080.000 ms
```

​			OpenGauss query plan:

```
Aggregate  (cost=499399.52..499399.53 rows=1 width=40) (actual time=4236.513..4236.513 rows=1 loops=1)
  ->  Seq Scan on synthetic_fraud_data  (cost=0.00..480687.01 rows=7485001 width=8) (actual time=0.994..2989.289 rows=7483766 loops=1)
Total runtime: 4237.092 ms
```

​	(iii)My opinion on the result

​		As we can see, Postgres runs faster than OpenGauss in aggregate function in this scenario.

​		It seems that Postgres's query optimizer works better in this scene.Postgres uses parallel scan instead of sequence scan like OpenGauss.



**4.Data importation speed from large csv or other form using COPY command**

​	(1)design of the experiment

​		Import a large dataset(size: approximately 2 GB) to Postgres and OpenGauss using COPY command.

​		Record the importation time and the system resources usage.

​	(2)result

| Postgres import time | OpenGauss import time |
| -------------------- | --------------------- |
| 2 min 48s 369ms      | 4 min 2s 406ms        |

​		Postgres resource usage:

![](/Users/davidwu/Desktop/project3/Screenshot 2024-12-13 at 22.39.02.png)



​		OpenGauss resource usage:

![](/Users/davidwu/Desktop/project3/Screenshot 2024-12-13 at 22.38.51.png)

​	(3) result analyze

​		$t_{Postgres} \approx \frac{1}{2}t_{OpenGauss}$ 

​		Posgres is super fast in data importing compared to OpenGauss.

​		Postgres uses about 25% of cpu during importing process.

​		OpenGauss uses about 37% of cpu during importing process.

​		As we can see the gap between OpenGauss and Postgres in this experiment is not as large as the gap in the next direct insert experiment. Since the COPY command may be the most efficient way to import large dataset.

**5.Direct bulk insert porformance**

​	(1)design of the experiment

​		We just create the table and directly use the insert command to do the bulk insert.

​		We would like to insert 1000000 record through insert command.

​		Repeat the experiment 5 times.

​		We shall use the following java code:

```python
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
```

​	(2)result

| Time | OpenGauss time(s) | Postgres time(s) |
| ---- | ----------------- | ---------------- |
| 1    | 50.353            | 5.039            |
| 2    | 51.434            | 4.771            |
| 3    | 51.223            | 5.311            |
| 4    | 50.251            | 4.797            |
| 5    | 51.434            | 5.124            |

​	(3)analyze

​		As we can see OpenGauss preforms extremely bad in this area.

​		So I think this aspect may be one of the draw back of OpenGauss.

**6.Direct bulk update**

​	(1)design of the experiment

​		We will use a dataset of 1000000 rows.

​		We will use 1000000 Update commands,but execute them in batch. 

​		We will use the following java code to initialize the data and proform update test:

```
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class BulkUpdateTestWithInit {
    public static void main(String[] args) {
        // 数据库连接配置
        String dbName = "postgres";
        String dbUser = "gaussdb";
        String dbPassword = "Secretpassword@123";
        String dbHost = "localhost";
        String dbPort = "15433";
        String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

        // 测试数据量和批量大小
        int totalRows = 1000000;
        int batchSize = 10000;

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            // 初始化表格
            initializeTable(conn, totalRows);

            // 测试 Bulk Update
            long updateTime = bulkUpdateTest(conn, totalRows, batchSize);

            // 输出耗时
            System.out.println("Bulk Update 用时: " + updateTime / 1000.0 + " 秒");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 初始化测试表格
    private static void initializeTable(Connection conn, int totalRows) throws Exception {
        String dropTableSQL = "DROP TABLE IF EXISTS bulk_test";
        String createTableSQL = "CREATE TABLE bulk_test (" +
                "id SERIAL PRIMARY KEY, " +
                "category INT, " +
                "data TEXT)";
        String insertSQL = "INSERT INTO bulk_test (category, data) VALUES (?, ?)";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(dropTableSQL);
            stmt.executeUpdate(createTableSQL);
        }

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            conn.setAutoCommit(false);

            for (int i = 0; i < totalRows; i++) {
                pstmt.setInt(1, i % 10); // 分类数据
                pstmt.setString(2, "random_data_" + i);
                pstmt.addBatch();

                if (i % 1000 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }
        System.out.println("表格初始化完成，插入了 " + totalRows + " 行数据");
    }

    // 执行 Bulk Update 测试
    private static long bulkUpdateTest(Connection conn, int totalRows, int batchSize) throws Exception {
        String updateSQL = "UPDATE bulk_test SET data = ? WHERE category = ?";
        long startTime = System.currentTimeMillis();

        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            conn.setAutoCommit(false);

            for (int i = 0; i < totalRows / batchSize; i++) {
                pstmt.setString(1, "updated_data");
                pstmt.setInt(2, i % 10); // 假设按 category 更新
                pstmt.addBatch();

                if (i % batchSize == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
}
```

​	(2)result

| Time | OpenGauss(s) | Postgres(s) |
| ---- | ------------ | ----------- |
| 1    | 70.795       | 39.066      |
| 2    | 70.665       | 39.316      |
| 3    | 70.797       | 38.404      |
| 4    | 71.345       | 39.517      |
| 5    | 72.385       | 39.342      |

​		Postgres's bulk update proformance is better than OpenGauss.

**7.Direct bulk delete**

​	(1)design of the experiment

​		We will create a dataset of 1000000 rows.

​		We will execute Delete commands 1000000 times.

​		Record the total execution time.

​		Use the following java code to porform the experiment:

```
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class BulkDeleteTestWithInit {
    public static void main(String[] args) {
        // 数据库连接配置
        String dbName = "postgres";
        String dbUser = "gaussdb";
        String dbPassword = "Secretpassword@123";
        String dbHost = "localhost";
        String dbPort = "15433";
        String url = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

        // 测试数据量和批量大小
        int totalRows = 1000000;
        int batchSize = 10000;

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            // 初始化表格
            initializeTable(conn, totalRows);

            // 测试 Bulk Delete
            long deleteTime = bulkDeleteTest(conn, totalRows, batchSize);

            // 输出耗时
            System.out.println("Bulk Delete 用时: " + deleteTime / 1000.0 + " 秒");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 初始化测试表格
    private static void initializeTable(Connection conn, int totalRows) throws Exception {
        String dropTableSQL = "DROP TABLE IF EXISTS bulk_test";
        String createTableSQL = "CREATE TABLE bulk_test (" +
                "id SERIAL PRIMARY KEY, " +
                "category INT, " +
                "data TEXT)";
        String insertSQL = "INSERT INTO bulk_test (category, data) VALUES (?, ?)";

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(dropTableSQL);
            stmt.executeUpdate(createTableSQL);
        }

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            conn.setAutoCommit(false);

            for (int i = 0; i < totalRows; i++) {
                pstmt.setInt(1, i % 10); // 分类数据
                pstmt.setString(2, "random_data_" + i);
                pstmt.addBatch();

                if (i % 1000 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }
        System.out.println("表格初始化完成，插入了 " + totalRows + " 行数据");
    }

    // 执行 Bulk Delete 测试
    private static long bulkDeleteTest(Connection conn, int totalRows, int batchSize) throws Exception {
        String deleteSQL = "DELETE FROM bulk_test WHERE category = ?";
        long startTime = System.currentTimeMillis();

        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            conn.setAutoCommit(false);

            for (int i = 0; i < totalRows / batchSize; i++) {
                pstmt.setInt(1, i % 10); // 假设按 category 删除
                pstmt.addBatch();

                if (i % batchSize == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        }

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
}
```

​	(2)result

| Time | OpenGauss(s) | Postgres(s) |
| ---- | ------------ | ----------- |
| 1    | 3.487        | 1.086       |
| 2    | 3.469        | 1.177       |
| 3    | 3.464        | 1.121       |
| 4    | 3.589        | 1.094       |
| 5    | 3.121        | 1.112       |

​	Postgres proforms better here.

**8.Index proformance**

​	(1)design of the experiment 

​		We want to measure the execution time of creating index.

​		We want to measure how large improvement can index offer in data retrieving.

​		We use imdb movie dataset in this experiment.

​		We will record the execution time before and after creating index on the startyear column.	

```sql
CREATE INDEX idx_startyear ON title_basics(startyear);
DROP INDEX idx_startyear;

SELECT * FROM title_basics ORDER BY startyear;
SELECT COUNT(*) FROM title_basics WHERE startyear = 2020;
SELECT startyear, COUNT(*) FROM title_basics GROUP BY startyear;

EXPLAIN SELECT * FROM title_basics ORDER BY startyear;
EXPLAIN SELECT COUNT(*) FROM title_basics WHERE startyear = 2020;
EXPLAIN startyear, COUNT(*) FROM title_basics GROUP BY startyear;
```

​	(2)result

​		index creation time:

| Time | OpenGauss creation time(ms) | Postgres creation time(ms) |
| ---- | --------------------------- | -------------------------- |
| 1    | 5836                        | 2348                       |
| 2    | 5833                        | 2237                       |
| 3    | 5746                        | 2240                       |
| 4    | 5834                        | 2317                       |
| 5    | 5835                        | 2315                       |

​		Postgres can create the index more quickly.		

​		First select command:

| Time | OpenGauss before/after index creation(ms) | Postgres before/after index creation(ms) |
| ---- | ----------------------------------------- | ---------------------------------------- |
| 1    | 3033/3130                                 | 2468/5                                   |
| 2    | 3134/3100                                 | 2513/6                                   |
| 3    | 3050/3034                                 | 2450/5                                   |
| 4    | 3157/3035                                 | 2464/7                                   |
| 5    | 3134/3012                                 | 2468/5                                   |

​		 query plan of OpenGauss after creating index: still Seq Scan(fail to use index in this scnenrio)

​		 query plan of Postgres after creating index: Index Scan(query optimizer decide to use index scan)

​	         This could be one of the draw back of OpenGauss's query optimizer.

​		 

​		Second select command:

| Time | OpenGauss before/after index creation(ms) | Postgres before/after index creation(ms) |
| ---- | ----------------------------------------- | ---------------------------------------- |
| 1    | 817/14                                    | 430/64                                   |
| 2    | 892/14                                    | 418/50                                   |
| 3    | 852/13                                    | 467/47                                   |
| 4    | 817/14                                    | 586/46                                   |
| 5    | 852/14                                    | 463/44                                   |

​		query plan of OpenGauss after creating index: Bitmap Index Scan

​		query plan of Postgres after creating index: Parallel Index Scan

​		Before creating index, OpenGauss is slower than Postgres in this part.

​		However, after creating index, OpenGauss works better than Postgres.Although Postgres uses parallel index scan.Postgres is still slower than OpenGauss in this part after creating index.



​		Last select command:

| Time | OpenGauss before/after index creation(ms) | Postgres before/after index creation(ms) |
| ---- | ----------------------------------------- | ---------------------------------------- |
| 1    | 1309/1310                                 | 891/372                                  |
| 2    | 1360/1314                                 | 855/375                                  |
| 3    | 1254/1320                                 | 854/370                                  |
| 4    | 1360/1314                                 | 881/375                                  |
| 5    | 1265/1340                                 | 809/377                                  |

​		 query plan of OpenGauss after creating index: still Seq Scan(fail to use index in this scnenrio)

​		 query plan of Postgres after creating index: Parallel Index Scan(query optimizer decide to use parallel index scan)

​		Again I think OpenGauss' s query optimizer is a bit naive compared to Postgres.(I have set the DOP of OpenGauss to be 4.So its optimizer should think it should use parallel index scan.)				

**9.Transaction per second porformance(TPS) when the size of data increases(One of the scalar test)**

​	(1) design of the experiment

​		We use pgbench to test TPS of Postgres and Opengauss with different scalar factor and remain the user number and thread number the same.

​		The standards of scalar factor is as following:

```
scalar factor = 1 is like this:
table                   # of rows
---------------------------------
pgbench_branches        1
pgbench_tellers         10
pgbench_accounts        100000
pgbench_history         0
```

​		We set the user number to be 50 and threads to be 3 and evaluation time to be 30 seconds  

​		In postgresql.conf, we set max_connection to be 200 and shared_buffer to be 1024MB both on Postgres and OpenGauss. 

​		The test is based on TPC-B.

​		The transaction runs during the test is like this:

​		This gives us a comprehensive view of database porformence.

```sql
BEGIN;

UPDATE pgbench_accounts SET abalance = abalance + :delta WHERE aid = :aid;

SELECT abalance FROM pgbench_accounts WHERE aid = :aid;

UPDATE pgbench_tellers SET tbalance = tbalance + :delta WHERE tid = :tid;

UPDATE pgbench_branches SET bbalance = bbalance + :delta WHERE bid = :bid;

INSERT INTO pgbench_history (tid, bid, aid, delta, mtime) VALUES (:tid, :bid, :aid, :delta, CURRENT_TIMESTAMP);

END;


```

​		We use the following command to start pgbench.

```
export PGPASSWORD="Secretpassword@123"
pgbench -i -s <scalar_factor> -h localhost -p 15433 -U gaussdb postgres
pgbench -h localhost -p 15433 -U gaussdb -T 30 -c 50 -j 3 postgres

export PGPASSWORD="My:s3Cr3t/"
pgbench -i -s <scalar_factor>  -h localhost -p 15434 -U postgres postgres
pgbench -h localhost -p 15434 -U postgres -T 30 -c 50 -j 3 postgres
```

​		(2)result

| Scalor factor | OpenGauss TPS | Postgres TPS |
| ------------- | ------------- | ------------ |
| 1             | 1550.756612   | 2616.182356  |
| 10            | 2331.740393   | 3635.785244  |
| 30            | 2282.221387   | 3719.718299  |
| 60            | 2392.789160   | 3420.745478  |
| 100           | 2168.792034   | 3149.889327  |
| 200           | 2153.672258   | 2848.760373  |

​		(3)analyze

​			In this part still, Postgres preforms better than OpenGauss.Under every size of data above, Postgres's TPS is larger than OpenGauss's TPS.The TPS of Postgres is approximately 1.5 times of the TPS of OpenGauss under any size of data above. 

​			As we can see, when the amount of data increases, both OpenGauss and Postgres have a quite stable porformance.

​			The size of data in a company's database will usually keep increasing in real life production environment.So if a DBMS can maintain its proformance when the size of data keeps increasing becomes an important standard of DBMS .

​			Conclusion: They both have a stable proformance when the size of data increases.However Postgres works better overall.	 



**10.Transaction per second proformance(TPS), under High-Concurrency Scenario**

​	(1) design of the experiment

​		Test the TPS porformance of OpenGauss and Postgres under different size of high concurrency scenario.

​		Change the size of cilent and remain the scalar factor and thread number the same.

​		This test is also based on the TPC-B.

​		We set the test time to be 30s, scalar factor to be 100, thread to be 3.

​		In postgresql.conf, we set max_connection to be 200 and shared_buffer to be 1024MB both on Postgres and OpenGauss. 

​		The standard of scalar factor is same as before.

​		Use the following command to start the pg_bench:		

```
export PGPASSWORD="Secretpassword@123"
pgbench -i -s 100 -h localhost -p 15433 -U gaussdb postgres  
pgbench -h localhost -p 15433 -U gaussdb -T 30 -c <number_of_user> -j 3 postgres

export PGPASSWORD="My:s3Cr3t/"
pgbench -i -s 100 -h localhost -p 15434 -U postgres postgres
pgbench -h localhost -p 15434 -U postgres -T 30 -c <number_of_user> -j 3 postgres
```

​	(2) result

| Number of concurrent user | OpenGauss TPS | Postgres TPS |
| ------------------------- | ------------- | ------------ |
| 20                        | 2591.123134   | 3370.893409  |
| 40                        | 2469.945100   | 3215.742562  |
| 80                        | 2278.612959   | 3020.568899  |
| 100                       | 2075.560024   | 2705.329057  |
| 120                       | 1973.000610   | 2635.266163  |
| 140                       | 1961.588444   | 2545.892586  |
| 180                       | 1735.711142   | 2240.478363  |
| 200                       | 1569.521492   | 2041.529580  |

​		(3) analyze

​			When the number of concurrent user increases, TPS of both OpenGauss and Postgres decrease.

​			OpenGauss's TPS may decrease slightly faster than Postgres's TPS.

​			With same amount of concurrent user, Postgres has a better porformance than OpenGauss.

**11.Latency of transaction**

​	(1) design of the experiment

​		Test the average latency of the transaction.

​		Shared buffer is set to 1024MB and max connection is set to 200 on both OpenGauss and Postgres.

​		Set the scaling factor to 100.

​		Set the evaluation time to be 30s and the number of user to be 75 and the threads number to be 3

```
export PGPASSWORD="Secretpassword@123"
pgbench -i -s 100 -h localhost -p 15433 -U gaussdb postgres  
pgbench -h localhost -p 15433 -U gaussdb -T 30 -c 75 -j 3 postgres

export PGPASSWORD="My:s3Cr3t/"
pgbench -i -s 100 -h localhost -p 15434 -U postgres postgres
pgbench -h localhost -p 15434 -U postgres -T 30 -c 75 -j 3 postgres
```

​	(2)result

| Times | OpenGauss avg latency(ms) | Postgres avg latency(ms) |
| ----- | ------------------------- | ------------------------ |
| 1     | 32.204                    | 23.225                   |
| 2     | 34.757                    | 22.775                   |
| 3     | 34.757                    | 23.225                   |
| 4     | 32.208                    | 23.112                   |
| 5     | 31.404                    | 22.776                   |

​	(3)analysis of the result

​		Average latency can reflects how long a customer will wait when the database processing their transaction.

​		As we can see, when OpenGauss and Postgres work on the same load, Postgres preforms better than OpenGauss.



**12.Try to optimize OpenGauss's proformance**

​	(1)design of the experiment 

​		In PostgreSQL.conf we modify the following setting:

```
shared_buffers = from 1024MB to 2048MB
wal_level= from logical to minimal
work_mem= from 64MB to 128MB
maintenance_work_mem = from 16MB to 128MB

```

​		And let's see if its proformance is optimized compared to its previous proformance in our previous experiment.

​	(2)result

​		(i)Data importation speed from large csv or other form using COPY command

| Previous       | Now             | Postgres        |
| -------------- | --------------- | --------------- |
| 4 min 2s 406ms | 2 min 43s 347ms | 2 min 48s 369ms |

​			Wow this is a great improvement......lol

​		(ii)count() function

| Previous(ms) | Now(ms) | Postgres(ms) |
| ------------ | ------- | ------------ |
| 2715         | 1699    | 461          |

​			Much better now, but still worse than Postgres.

​		(iii)sum() function

| Previous(ms) | Now(ms) | Postgres(ms) |
| ------------ | ------- | ------------ |
| 2421         | 1671    | 720          |

​		(iv)avg() function

| Previous(ms) | Now(ms) | Postgres(ms) |
| ------------ | ------- | ------------ |
| 3149         | 2169    | 820          |

​		(v)direct bulk insert

| Previous(s) | Now(s) | Postgres(s) |
| ----------- | ------ | ----------- |
| 50          | 44.636 | 5           |

​			Very tiny mprovement.

​		(vi)direct bulk update

| Previous(s) | Now(s) | Postgres(s) |
| ----------- | ------ | ----------- |
| 70          | 50     | 39          |

​			medium improvement		

​		(vii)index creation

| Previous(ms) | Now(ms) | Postgres(ms) |
| ------------ | ------- | ------------ |
| 5833         | 3310    | 2240         |

​			medium improvement

​		(viii)TPS(client:100 , scaling factor:100, threads 3, time:30s)

| Previous TPS | Now TPS     | Postgres TPS |
| ------------ | ----------- | ------------ |
| 2075         | 2628.806310 | 2705         |

​			表现逼近postgres的表现了。

​	(3)summary

​		I found that increasing the resoure allowcated to OpenGauss can effectively optimize its proformance.

​		The default value of these parameters we modified here of Postgres is so much lower than the current value of OpenGauss here.(eg: {Postgres defaut shared_buffers=128MB, here share_buffers=2GB},{Postgres defaut work_mem=4MB,here work_mem=128MB}).However,  OpenGauss barely achieved the proformance of Postgres.It seems that OpenGauss need more resource to achieve the same proformance of Postgres.



**Part III. Detailed Evaluation of OpenGauss**

​	(i)Strengths of OpenGauss:

​		(1)**Performance Consistency**: OpenGauss demonstrates stable performance even with increasing data size, maintaining consistent TPS under varying scalar factors**(Experiment 9)**.

​		(2)**Data Retrieval with Indexing**: After creating indexes, openGauss outperformed PostgreSQL in some scenarios, such as retrieving grouped data**(Experiment 8 the second select command).**

​		(3)**It's proformance can be effectively improved by increasing the resources allocation **:Adjusting parameters (e.g., `shared_buffers`, `work_mem`) significantly enhances performance in some of the aspect, bridging the gap with PostgreSQL in some aspects**(Experiment 12).**

​		(4)**Large join porformance:** Its large join porformance is nearly as well as Postgres.**(Experiment 2)**

​	 (ii)Weekness of OpenGauss

​		(1)**Compared to Postgress it is still too week**:OpenGauss generally lags behind Postgres in most tests, including basic queries, aggregate functions, and transaction latency and TPS scalar test so on**(according to all of the experiment).**

​		(2)**Its query optimization is quite poor**: Remember that we checked the query plan in some of the experiment above.We found that Opengauss's query optimizer is quite naive compared to Postgres.

​			For example: In the first and last scenarios of experiment 8, openGauss fails to utilize indexes effectively.What's more, in experiment 3, OpenGauss failed to decide to use parallel scan even when the DOP is set larger than 1(Postgres make the right choice).**(Experiment 3,Experiment 8)**

​		(3)**Resources intensity**:Even with increased resource allocation, openGauss struggles to match Postgres’s performance.It demands significantly more resources to approach similar levels of performance, suggesting inefficiencies in resource utilization.**(Experiment12)**

​		(4)**Data Import and Bulk Operations:**Bulk operations (insert, update, delete) are notably slower than Postgres**(Experiment 5, Experiment 6, Experiment 7).** Bulk data importation using COPY command is also signicantly slower than Postgres**(Experiment4)**

​		(5)**High-Concurrency Performance:** Opengauss works worse than Postgres in any number of concurrency users.What's more, when the number of concurrency users increases, OpenGauss's TPS decreases slightly faster than Postgres**(Experiment 10).**

​		(6)**Transaction latency proformance**: OpenGauss has a higher transaction latency than Postgres on the same work load.This could lower the user's experience in the real production environment.**(Experiment11)**

​		(7)**Defaut postgresql.conf gap:**As we can see in the experiment above, Postgres could perform quite good with its defaut configuration.However, OpenGauss requires significant manual tuning to optimize performance.Perhaps Postgres can save us a lot of time**(according to all of the experiment above and experiment 12)**



**Part IV: Summary**

​	In conclusion, while OpenGauss shows potential in certain areas, there is still a long way to go to match leading systems like PostgreSQL.**Huawei** developers must approach development with **focus and dedication**, **fostering innovation rather than merely replicating PostgreSQL and producing a significantly less performant copy.** True progress will come from building unique strengths and addressing the system's fundamental limitations.



​		
