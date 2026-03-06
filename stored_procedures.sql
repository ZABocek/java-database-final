USE inventory;

DELIMITER $$

DROP PROCEDURE IF EXISTS GetMonthlySalesForEachStore $$
CREATE PROCEDURE GetMonthlySalesForEachStore(IN year_param INT, IN month_param INT)
BEGIN
    SELECT od.store_id,
           SUM(DISTINCT od.total_price) AS total_sales,
           MONTH(od.date) AS sale_month,
           YEAR(od.date) AS sale_year
    FROM order_details od
    WHERE YEAR(od.date) = year_param
      AND MONTH(od.date) = month_param
    GROUP BY od.store_id, MONTH(od.date), YEAR(od.date)
    ORDER BY total_sales DESC;
END $$

DROP PROCEDURE IF EXISTS GetAggregateSalesForCompany $$
CREATE PROCEDURE GetAggregateSalesForCompany(IN year_param INT, IN month_param INT)
BEGIN
    SELECT SUM(DISTINCT od.total_price) AS total_sales,
           MONTH(od.date) AS sale_month,
           YEAR(od.date) AS sale_year
    FROM order_details od
    WHERE YEAR(od.date) = year_param
      AND MONTH(od.date) = month_param
    GROUP BY MONTH(od.date), YEAR(od.date);
END $$

DROP PROCEDURE IF EXISTS GetTopSellingProductsByCategory $$
CREATE PROCEDURE GetTopSellingProductsByCategory(IN target_month INT, IN target_year INT)
BEGIN
    SELECT t.category,
           t.name,
           t.total_quantity_sold,
           t.total_sales
    FROM (
        SELECT p.category,
               p.name,
               SUM(oi.quantity) AS total_quantity_sold,
               SUM(oi.quantity * oi.price) AS total_sales,
               DENSE_RANK() OVER (PARTITION BY p.category ORDER BY SUM(oi.quantity) DESC) AS rnk
        FROM order_item oi
        JOIN order_details od ON od.id = oi.order_id
        JOIN product p ON p.id = oi.product_id
        WHERE MONTH(od.date) = target_month
          AND YEAR(od.date) = target_year
        GROUP BY p.category, p.name
    ) t
    WHERE t.rnk = 1
    ORDER BY t.category, t.name;
END $$

DROP PROCEDURE IF EXISTS GetTopSellingProductByStore $$
CREATE PROCEDURE GetTopSellingProductByStore(IN target_month INT, IN target_year INT)
BEGIN
    SELECT t.product_name,
           t.store_id,
           t.total_quantity_sold,
           t.total_sales
    FROM (
        SELECT p.name AS product_name,
               od.store_id,
               SUM(oi.quantity) AS total_quantity_sold,
               SUM(oi.quantity * oi.price) AS total_sales,
               DENSE_RANK() OVER (PARTITION BY od.store_id ORDER BY SUM(oi.quantity) DESC) AS rnk
        FROM order_item oi
        JOIN order_details od ON od.id = oi.order_id
        JOIN product p ON p.id = oi.product_id
        WHERE MONTH(od.date) = target_month
          AND YEAR(od.date) = target_year
        GROUP BY p.name, od.store_id
    ) t
    WHERE t.rnk = 1
    ORDER BY t.store_id, t.product_name;
END $$

DELIMITER ;
