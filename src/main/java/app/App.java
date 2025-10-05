package app;

import org.w3c.dom.ls.LSOutput;

import java.sql.*;
import java.util.*;

public class App {
    private Connection connection;
    private Scanner scanner;

    private void connect() throws SQLException {
        this.connection = Db.getConnection();
    }

    private void disconnect() throws SQLException {
        this.connection.close();
    }

    private String getStringInput(String caption, boolean allowEmpty) {
        System.out.print(caption + ": ");

        while (true) {
            String result = this.scanner.nextLine().strip();

            if (!allowEmpty && result.isEmpty()) {
                System.out.println("Поле не может быть пустым!");
                continue;
            }

            return result;
        }


    }

    private String getStringInput(String caption) {
        return this.getStringInput(caption, true);
    }

    private int getPositiveIntegerInput(String caption){
        while(true) {
            System.out.print(caption + ": ");

            try {
                int result = Integer.parseInt(this.scanner.nextLine());

                if (result < 0) {
                    throw new NumberFormatException();
                }

                return result;
            } catch (NumberFormatException e) {
                System.out.print("Введите целое положительное число!");
            }
        }
    }

    private String getStringInputUniq(String caption, String sql) throws SQLException {
        String result;

        try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
            while (true) {
                System.out.print(caption + ": ");
                result = scanner.nextLine().strip();

                if (result.isEmpty()) {
                    System.out.println("Поле не должно быть пустым!");
                    continue;
                }

                ps.setString(1, result);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Элемент с таким значением уже есть!");
                        continue;
                    } else {
                        return result;
                    }
                }
            }
        }
    }

    private void addProduct() {
        System.out.println("Добавление нового продукта!");

        try (PreparedStatement ps = this.connection.prepareStatement("""
            insert into product (name, description, price, quantity, category)
            values (?, ?, ?, ?, ?)
            returning id
        """)) {
            ps.setString(1, this.getStringInputUniq("Название", "select id from product where name = ?"));
            ps.setString(2, this.getStringInput("Описание"));
            ps.setInt(3, this.getPositiveIntegerInput("Цена"));
            ps.setInt(4, this.getPositiveIntegerInput("Количество"));
            ps.setString(5, this.getStringInput("Категория"));

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Продукт создан, id=" + rs.getLong(1));
                } else {
                    System.out.println("Продукт не создан почему-то!");
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка создания продукта: " + e.getMessage());
        }
    }

    private void addCustomer() {
        System.out.println("Добавление нового покупателя!");

        try (PreparedStatement ps = this.connection.prepareStatement("""
            insert into customer (first_name, last_name, phone, email)
            values (?, ?, ?, ?)
            returning id
        """)) {
            ps.setString(1, this.getStringInput("Имя", false));
            ps.setString(2, this.getStringInput("Фамилия", false));

            // тут можно добавить проверку формата регуляркой, но не хочется(раз по заданию не требуется)
            ps.setString(3, this.getStringInputUniq("Телефон", "select id from customer where phone = ?"));
            ps.setString(4, this.getStringInputUniq("Email", "select id from customer where email = ?"));

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Пользователь создан, id=" + rs.getLong(1));
                } else {
                    System.out.println("Пользователь не создан почему-то!");
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка создания пользователя: " + e.getMessage());
        }
    }

    private void createOrder() throws SQLException {
        int customer_id;
        int product_id;
        int quantity;

        try (Statement smt = this.connection.createStatement()) {
            Map<Integer, String> customers = new HashMap<>();

            try(ResultSet rs = smt.executeQuery("select id, first_name, last_name, email from customer order by id")) {
                while (rs.next()) {
                    String customer_info =
                        rs.getString("first_name") + " " + rs.getString("last_name")
                            + " (" + rs.getString("email") + ")";

                    System.out.println(rs.getString("id") + ": " + customer_info);
                    customers.put(rs.getInt("id"), customer_info);
                }
            }

            while (true) {
                customer_id = this.getPositiveIntegerInput("Выберите покупателя");

                if (customers.containsKey(customer_id)) {
                    break;
                } else {
                    System.out.println("Недопустимый ид покупателя!");
                }
            }

            Map<Integer, String> products = new HashMap<>();
            Map<Integer, Integer> products_quantity = new HashMap<>();

            try (ResultSet rs = smt.executeQuery("select id, name, quantity from product where quantity > 0 order by id")) {
                while (rs.next()) {
                    System.out.println(rs.getString("id") + ": " + rs.getString("name") + " (на складе " + rs.getString("quantity") + ")");
                    products.put(rs.getInt("id"), rs.getString("name"));
                    products_quantity.put(rs.getInt("id"), rs.getInt("quantity"));
                }
            }

            while (true) {
                product_id = this.getPositiveIntegerInput("Выберите товар");

                if (products.containsKey(product_id)) {
                    break;
                } else {
                    System.out.println("Недопустимый ид товара!");
                }
            }

            while (true) {
                quantity = this.getPositiveIntegerInput("Количество");

                if (quantity > products_quantity.get(product_id)) {
                    System.out.println("Недостаточно товара, на складе всего " + products_quantity.get(product_id));
                } else {
                    break;
                }
            }
        }

        // создание заказа - изменение в двух таблицах, поэтому делаем через транзакцию
        try {
            connection.setAutoCommit(false);

            int new_order_id;

            // препереды делать не стал, так как все на числовых данных. очень они неудобные
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(String.format("update product set quantity = quantity - %d where id = %d", quantity, product_id));

                try (ResultSet rs = stmt.executeQuery(String.format(
                    "insert into \"order\" (product_id, customer_id, quantity, status_id)" +
                    "values (%d, %d, %d, 1) returning id",
                    product_id, customer_id, quantity
                ))) {
                    if (rs.next()) {
                        new_order_id = rs.getInt("id");
                    } else {
                        throw new SQLException("Что-то пошло не так");
                    }
                }
            }

            connection.commit();

            System.out.println("Создан новый заказ: id=" + new_order_id);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e2) {
                e2.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateProduct() throws SQLException {
        // я понимаю что лучше было бы разнести отдельно установку количества и цены, но нет

        int product_id;
        int new_quantity;
        int new_price;

        try (Statement smt = this.connection.createStatement()) {
            Map<Integer, String> products = new HashMap<>();

            try(ResultSet rs = smt.executeQuery("select id, name, quantity, price from product order by id")) {
                while (rs.next()) {
                    System.out.println(
                        rs.getString("id") + ": " + rs.getString("name") +
                            " [цена " + rs.getString("price") + " шекелей]" +
                            " (на складе " + rs.getString("quantity") + ")");
                    products.put(rs.getInt("id"), rs.getString("name"));
                }
            }

            while (true) {
                product_id = this.getPositiveIntegerInput("Выберите товар");

                if (products.containsKey(product_id)) {
                    break;
                } else {
                    System.out.println("Недопустимый ид товара!");
                }
            }

            new_quantity = this.getPositiveIntegerInput("Количество");
            new_price = this.getPositiveIntegerInput("Цена");

            smt.executeUpdate(String.format("update product set quantity = %d, price = %d where id = %d",  new_quantity, new_price, product_id));
        }
    }

    private void deleteOrder() throws SQLException {
        int order_id;
        // да, тут нужна структура
        Map<Integer, String> order_status = new HashMap<>();
        Map<Integer, List<Integer>> order_product_quantity = new HashMap<>();

        try (Statement smt = this.connection.createStatement()) {
            try (ResultSet rs = smt.executeQuery("""
                select o.id, o.order_date, p.name, o.quantity, c.last_name, os.status_name, o.product_id
                from
                    "order" o
                    left join order_status os on o.status_id = os.id
                    left join product p on p.id = o.product_id
                    left join customer c on c.id = o.customer_id
                order by
                    o.order_date desc
                limit 10 -- не хочу пагинировать
            """)) {
                while (rs.next()) {
                    System.out.println(
                        rs.getString("id") + ": " + rs.getString("name")
                            + " (" + rs.getString("quantity") + ") - " + rs.getString("last_name")
                    );

                    order_status.put(rs.getInt("id"), rs.getString("status_name"));
                    order_product_quantity.put(rs.getInt("id"), List.of(rs.getInt("product_id"), rs.getInt("quantity")));
                }
            }

            while (true) {
                order_id = this.getPositiveIntegerInput("Выберите заказ");

                if (order_status.containsKey(order_id)) {
                    break;
                } else {
                    System.out.println("Недопустимый ид заказа!");
                }
            }
        }

        // удаление заказа - возможно изменение в двух таблицах, поэтому делаем через транзакцию
        try {
            connection.setAutoCommit(false);

            try (Statement stmt = connection.createStatement()) {
                if (List.of("создан", "в обработке", "отправлен").contains(order_status.get(order_id))) {
                    stmt.executeUpdate(String.format("update product set quantity = quantity + %d where id = %d", order_product_quantity.get(order_id).get(1), order_product_quantity.get(order_id).get(0)));
                }

                stmt.executeUpdate("delete from \"order\" where id = " + order_id);
            }

            connection.commit();

            System.out.println("Заказ удален!");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e2) {
                e2.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void run() throws SQLException {
        try {
            this.connect();

            this.scanner = new Scanner(System.in);

            while (true) {
                System.out.print("""
Что делаем?
1) Добавляем товар
2) Добавляем покупателя
3) Создать заказ
4) Изменить товар
5) Удалить заказ
Введите номер пункта: """);

                String input = this.scanner.nextLine();

                switch (input) {
                    case "1": this.addProduct(); break;
                    case "2": this.addCustomer(); break;
                    case "3": this.createOrder(); break;
                    case "4": this.updateProduct(); break;
                    case "5": this.deleteOrder(); break;

                    default: {
                        System.out.println("Нет такого пункта, попробуй ещо!");
                    }
                }
            }
        } finally {
            if (this.scanner != null) {
                this.scanner.close();
            }

            if (this.connection != null) {
                this.disconnect();
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        // Вроде плюс минус важные эксепшоны я обрабатываю внутри, так что наружу должно самое критичное пролетать
        // хотя уверен что что-то пропустил
        new App().run();
    }
}
