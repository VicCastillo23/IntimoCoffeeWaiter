package com.intimocoffee.waiter.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.intimocoffee.waiter.core.database.dao.*
import com.intimocoffee.waiter.core.database.entity.*

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        TableEntity::class,
        ReservationEntity::class,
        NotificationEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class IntimoCoffeeDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun tableDao(): TableDao
    abstract fun reservationDao(): ReservationDao
    abstract fun notificationDao(): NotificationDao
    
    companion object {
        @Volatile
        private var INSTANCE: IntimoCoffeeDatabase? = null
        
        // Migration from version 1 to 2: Add additional order fields
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to orders table
                database.execSQL("ALTER TABLE orders ADD COLUMN originalOrderId TEXT")
                database.execSQL("ALTER TABLE orders ADD COLUMN isAdditionalOrder INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        // Migration from version 2 to 3: Add reservations table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create reservations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reservations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tableId` INTEGER NOT NULL,
                        `customerName` TEXT NOT NULL,
                        `customerPhone` TEXT NOT NULL,
                        `customerEmail` TEXT,
                        `partySize` INTEGER NOT NULL,
                        `reservationDate` TEXT NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `notes` TEXT,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        `createdBy` INTEGER NOT NULL,
                        FOREIGN KEY(`tableId`) REFERENCES `tables`(`id`) ON DELETE CASCADE
                    )
                """)
                
                // Create index for tableId
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_reservations_tableId` ON `reservations` (`tableId`)")
            }
        }
        
        // Migration from version 3 to 4: Update users table with new fields
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new users table with updated structure
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `users_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `username` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `email` TEXT,
                        `phone` TEXT,
                        `role` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `hireDate` TEXT,
                        `salary` REAL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
                
                // Migrate existing data
                database.execSQL("""
                    INSERT INTO `users_new` (
                        `username`, `password`, `fullName`, `role`, `isActive`, `createdAt`, `updatedAt`
                    )
                    SELECT 
                        `username`, `password`, `fullName`, `role`, `isActive`, `createdAt`, `createdAt`
                    FROM `users`
                """)
                
                // Drop old table and rename new one
                database.execSQL("DROP TABLE `users`")
                database.execSQL("ALTER TABLE `users_new` RENAME TO `users`")
            }
        }
        
        // Migration from version 4 to 5: Add notifications table
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create notifications table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `readAt` INTEGER,
                        `isRead` INTEGER NOT NULL DEFAULT 0,
                        `metadata` TEXT
                    )
                """)
                
                // Create indices for efficient querying
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_type` ON `notifications` (`type`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_isRead` ON `notifications` (`isRead`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_createdAt` ON `notifications` (`createdAt`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_priority` ON `notifications` (`priority`)")
            }
        }
        
        // Migration from version 5 to 6: Add new OrderEntity fields
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to orders table for time tracking
                database.execSQL("ALTER TABLE orders ADD COLUMN tableName TEXT")
                database.execSQL("ALTER TABLE orders ADD COLUMN sentToKitchenAt INTEGER")
                database.execSQL("ALTER TABLE orders ADD COLUMN sentToBarAt INTEGER")
                database.execSQL("ALTER TABLE orders ADD COLUMN preparationStartedAt INTEGER")
                database.execSQL("ALTER TABLE orders ADD COLUMN readyAt INTEGER")
                database.execSQL("ALTER TABLE orders ADD COLUMN deliveredAt INTEGER")
                database.execSQL("ALTER TABLE orders ADD COLUMN completedAt INTEGER")
            }
        }
        
        fun getDatabase(context: Context): IntimoCoffeeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IntimoCoffeeDatabase::class.java,
                    "intimo_coffee_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration() // For development - recreates DB if migration fails
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}