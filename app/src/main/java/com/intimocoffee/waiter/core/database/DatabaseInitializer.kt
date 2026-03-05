package com.intimocoffee.waiter.core.database

import android.util.Log
import com.intimocoffee.waiter.core.database.entity.CategoryEntity
import com.intimocoffee.waiter.core.database.entity.ProductEntity
import com.intimocoffee.waiter.core.database.entity.TableEntity
import com.intimocoffee.waiter.core.database.entity.OrderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val database: IntimoCoffeeDatabase
) {
    
    suspend fun initializeDatabase() {
        withContext(Dispatchers.IO) {
            try {
                // Check if data already exists
                val tablesCount = database.tableDao().getAllActiveTables().first().size
                val productsCount = database.productDao().getAllActiveProducts().first().size
                
                Log.d("DatabaseInitializer", "Initializing database...")
                Log.d("DatabaseInitializer", "Found $tablesCount tables and $productsCount products")
                
                // Only clean if we want to force a fresh start (uncomment next line if needed)
                // cleanDatabase()
                
                // Initialize tables if not exist
                if (tablesCount == 0) {
                    initializeTables()
                }
                
                // Initialize categories if not exist
                if (productsCount == 0) {
                    initializeCategories()
                    initializeProducts()
                }
                
                // Initialize sample orders - COMMENTED OUT FOR CLEAN START
                // initializeSampleOrders()
                
                Log.d("DatabaseInitializer", "Database initialization completed")
                
            } catch (e: Exception) {
                Log.e("DatabaseInitializer", "Error initializing database", e)
            }
        }
    }
    
    private suspend fun cleanDatabase() {
        try {
            Log.d("DatabaseInitializer", "Cleaning existing database data...")
            
            // Clear orders and order items first (due to foreign key constraints)
            database.orderDao().deleteAllOrderItems()
            database.orderDao().deleteAllOrders()
            
            // Clear tables
            database.tableDao().deleteAllTables()
            
            // Clear products and categories
            database.productDao().deleteAllProducts()
            database.categoryDao().deleteAllCategories()
            
            Log.d("DatabaseInitializer", "Database cleaned successfully")
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error cleaning database", e)
        }
    }
    
    private suspend fun initializeTables() {
        Log.d("DatabaseInitializer", "Initializing tables...")
        
        val tables = listOf(
            TableEntity(
                id = "1",
                number = 1,
                name = "Mesa VIP",
                capacity = 4,
                zone = "VIP",
                status = "FREE",
                currentOrderId = null,
                positionX = 100f,
                positionY = 100f,
                isActive = true
            ),
            TableEntity(
                id = "2", 
                number = 2,
                name = null,
                capacity = 2,
                zone = "Principal",
                status = "FREE",
                currentOrderId = null,
                positionX = 200f,
                positionY = 100f,
                isActive = true
            ),
            TableEntity(
                id = "3",
                number = 3,
                name = null,
                capacity = 6,
                zone = "Principal", 
                status = "FREE",
                currentOrderId = null,
                positionX = 300f,
                positionY = 100f,
                isActive = true
            ),
            TableEntity(
                id = "4",
                number = 4,
                name = "Mesa Terraza",
                capacity = 4,
                zone = "Terraza",
                status = "RESERVED",
                currentOrderId = null,
                positionX = 150f,
                positionY = 200f,
                isActive = true
            ),
            TableEntity(
                id = "5",
                number = 5,
                name = null,
                capacity = 2,
                zone = "Principal",
                status = "FREE",
                currentOrderId = null,
                positionX = 250f,
                positionY = 200f,
                isActive = true
            ),
            TableEntity(
                id = "6",
                number = 6,
                name = null,
                capacity = 4,
                zone = "Principal",
                status = "FREE",
                currentOrderId = null,
                positionX = 350f,
                positionY = 200f,
                isActive = true
            ),
            TableEntity(
                id = "7",
                number = 7,
                name = null,
                capacity = 2,
                zone = "Terraza",
                status = "FREE",
                currentOrderId = null,
                positionX = 100f,
                positionY = 300f,
                isActive = true
            )
        )
        
        database.tableDao().insertTables(tables)
        Log.d("DatabaseInitializer", "Inserted ${tables.size} tables")
    }
    
    private suspend fun initializeCategories() {
        Log.d("DatabaseInitializer", "Initializing categories...")
        
        val categories = listOf(
            CategoryEntity(
                id = "1",
                name = "Bebidas Calientes",
                description = "Cafés, tés y otras bebidas calientes",
                color = "#8B4513",
                isActive = true,
                sortOrder = 1
            ),
            CategoryEntity(
                id = "2", 
                name = "Bebidas Frías",
                description = "Jugos, sodas y bebidas refrescantes",
                color = "#4169E1",
                isActive = true,
                sortOrder = 2
            ),
            CategoryEntity(
                id = "3",
                name = "Pasteles y Postres",
                description = "Tortas, pasteles y postres dulces",
                color = "#FF69B4",
                isActive = true,
                sortOrder = 3
            ),
            CategoryEntity(
                id = "4",
                name = "Snacks",
                description = "Bocadillos y comidas rápidas",
                color = "#FFA500",
                isActive = true,
                sortOrder = 4
            )
        )
        
        database.categoryDao().insertCategories(categories)
        Log.d("DatabaseInitializer", "Inserted ${categories.size} categories")
    }
    
    private suspend fun initializeProducts() {
        Log.d("DatabaseInitializer", "Initializing products...")
        
        val products = listOf(
            // Bebidas Calientes
            ProductEntity(
                id = "1",
                name = "Café Americano",
                description = "Café negro clásico",
                price = "2.50",
                categoryId = "1",
                imageUrl = null,
                isActive = true,
                stockQuantity = 100,
                minStockLevel = 20
            ),
            ProductEntity(
                id = "2",
                name = "Café Latte",
                description = "Café con leche vaporizada",
                price = "3.50",
                categoryId = "1",
                isActive = true,
                stockQuantity = 15,
                minStockLevel = 20
            ),
            ProductEntity(
                id = "3",
                name = "Cappuccino",
                description = "Café espresso con leche espumosa",
                price = "3.25",
                categoryId = "1",
                isActive = true,
                stockQuantity = 50,
                minStockLevel = 15
            ),
            
            // Bebidas Frías
            ProductEntity(
                id = "4",
                name = "Jugo de Naranja Natural",
                description = "Jugo fresco de naranjas",
                price = "2.75",
                categoryId = "2",
                isActive = true,
                stockQuantity = 0,
                minStockLevel = 10
            ),
            ProductEntity(
                id = "5",
                name = "Frappé de Chocolate",
                description = "Bebida helada de chocolate",
                price = "4.25",
                categoryId = "2",
                isActive = true
            ),
            
            // Pasteles y Postres
            ProductEntity(
                id = "6",
                name = "Cheesecake de Fresa",
                description = "Pastel de queso con fresas frescas",
                price = "4.50",
                categoryId = "3",
                isActive = true
            ),
            ProductEntity(
                id = "7",
                name = "Brownie de Chocolate",
                description = "Brownie húmedo con nueces",
                price = "3.75",
                categoryId = "3",
                isActive = true
            ),
            
            // Snacks
            ProductEntity(
                id = "8",
                name = "Sándwich Club",
                description = "Sándwich de pollo, bacon y verduras",
                price = "5.25",
                categoryId = "4",
                isActive = true
            ),
            ProductEntity(
                id = "9",
                name = "Croissant de Jamón",
                description = "Croissant relleno de jamón y queso",
                price = "3.50",
                categoryId = "4",
                isActive = true
            )
        )
        
        database.productDao().insertProducts(products)
        Log.d("DatabaseInitializer", "Inserted ${products.size} products")
    }
    
    private suspend fun initializeSampleOrders() {
        Log.d("DatabaseInitializer", "Initializing sample orders...")
        
        val currentTime = System.currentTimeMillis()
        
        val orders = listOf(
            OrderEntity(
                id = "order_1",
                orderNumber = 1,
                tableId = "1",
                userId = "user_admin",
                status = "PREPARING",
                subtotal = "12.50",
                tax = "1.25",
                discount = "0.00",
                total = "13.75",
                paymentMethod = null,
                paymentStatus = "PENDING",
                notes = "Sin azúcar en el café",
                createdAt = currentTime - (30 * 60 * 1000), // 30 minutos atrás
                updatedAt = currentTime - (15 * 60 * 1000)
            ),
            OrderEntity(
                id = "order_2",
                orderNumber = 2,
                tableId = "2",
                userId = "user_admin",
                status = "PENDING",
                subtotal = "8.25",
                tax = "0.83",
                discount = "0.00",
                total = "9.08",
                paymentMethod = null,
                paymentStatus = "PENDING",
                notes = null,
                createdAt = currentTime - (15 * 60 * 1000), // 15 minutos atrás
                updatedAt = currentTime - (15 * 60 * 1000)
            ),
            OrderEntity(
                id = "order_3",
                orderNumber = 3,
                tableId = "7",
                userId = "user_admin",
                status = "READY",
                subtotal = "15.75",
                tax = "1.58",
                discount = "0.00",
                total = "17.33",
                paymentMethod = null,
                paymentStatus = "PENDING",
                notes = "Para llevar",
                createdAt = currentTime - (45 * 60 * 1000), // 45 minutos atrás
                updatedAt = currentTime - (5 * 60 * 1000)
            ),
            OrderEntity(
                id = "order_4",
                orderNumber = 4,
                tableId = null,
                userId = "user_admin",
                status = "DELIVERED",
                subtotal = "25.50",
                tax = "2.55",
                discount = "2.00",
                total = "26.05",
                paymentMethod = "CASH",
                paymentStatus = "PAID",
                notes = "Orden completada",
                createdAt = currentTime - (2 * 60 * 60 * 1000), // 2 horas atrás
                updatedAt = currentTime - (90 * 60 * 1000)
            ),
            OrderEntity(
                id = "order_5",
                orderNumber = 5,
                tableId = null,
                userId = "user_admin",
                status = "PAID",
                subtotal = "18.25",
                tax = "1.83",
                discount = "0.00",
                total = "20.08",
                paymentMethod = "CARD",
                paymentStatus = "PAID",
                notes = "Cliente satisfecho",
                createdAt = currentTime - (3 * 60 * 60 * 1000), // 3 horas atrás
                updatedAt = currentTime - (2 * 60 * 60 * 1000)
            )
        )
        
        database.orderDao().insertOrder(orders[0])
        database.orderDao().insertOrder(orders[1])
        database.orderDao().insertOrder(orders[2])
        database.orderDao().insertOrder(orders[3])
        database.orderDao().insertOrder(orders[4])
        
        Log.d("DatabaseInitializer", "Inserted ${orders.size} sample orders")
    }
}