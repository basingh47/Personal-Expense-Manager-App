package com.example.data

data class CategoryDef(
    val name: String,
    val icon: String, // Material Icon name or visual emoji
    val subcategories: List<String>,
    val isCustom: Boolean = false
)

object CategoryData {
    val expenseCategories = listOf(
        CategoryDef(
            name = "Food",
            icon = "🍔",
            subcategories = listOf("Breakfast", "Lunch", "Dinner", "Tea/Coffee", "Snacks", "Groceries", "Delivery Apps", "Street Food", "Fine Dining")
        ),
        CategoryDef(
            name = "Vehicle",
            icon = "🛵",
            subcategories = listOf("Fuel", "Service", "Repair", "Insurance", "Accessories", "Washing", "Tyres", "Battery", "Tolls & Parking", "Challan / Fines")
        ),
        CategoryDef(
            name = "Home",
            icon = "🏠",
            subcategories = listOf("Rent", "Electricity", "Internet", "Water", "Maintenance", "LPG Gas", "House Help / Maid", "Furniture & Decor", "Home Insurance")
        ),
        CategoryDef(
            name = "Work / Office",
            icon = "💻",
            subcategories = listOf("Laptop & Gadgets", "Software / SaaS", "Office Supplies", "Co-working / Rent", "Business Travel")
        ),
        CategoryDef(
            name = "Education",
            icon = "📚",
            subcategories = listOf("College / School Fees", "Online Courses", "Books & Study Material", "Certifications", "Stationery")
        ),
        CategoryDef(
            name = "Entertainment",
            icon = "🎮",
            subcategories = listOf("Movies", "Games", "OTT Subscriptions", "Concerts", "Amusement Parks")
        ),
        CategoryDef(
            name = "Travel / Commute",
            icon = "✈️",
            subcategories = listOf("Flights", "Trains / Metro", "Cab / Taxi", "Hotels & Stays", "Luggage & Gear", "Sightseeing")
        ),
        CategoryDef(
            name = "Health",
            icon = "🏥",
            subcategories = listOf("Doctor Consult", "Medicines", "Gym / Fitness", "Health Insurance", "Lab Tests", "Dental Care", "Eye Wear", "Therapy")
        ),
        CategoryDef(
            name = "Shopping",
            icon = "🛍️",
            subcategories = listOf("Clothes & Apparel", "Electronics", "Gifts", "Personal Care & Cosmetics", "Footwear", "Jewelry / Accessories")
        ),
        CategoryDef(
            name = "Investments",
            icon = "📈",
            subcategories = listOf("Mutual Funds", "Stocks", "Fixed Deposits", "Gold", "Crypto", "Public Provident Fund (PPF)")
        ),
        CategoryDef(
            name = "Insurance / Loan",
            icon = "🛡️",
            subcategories = listOf("Life Insurance", "Term Insurance", "Home Loan EMI", "Car/Bike Loan EMI", "Personal Loan EMI", "Credit Card Bill")
        ),
        CategoryDef(
            name = "Pets",
            icon = "🐾",
            subcategories = listOf("Pet Food", "Vet Consultations", "Medicines / Vaccine", "Toys & Accessories", "Grooming")
        ),
        CategoryDef(
            name = "Charity & Gifts",
            icon = "🎁",
            subcategories = listOf("Donations", "Family Gifts", "Friend Celebrations", "Tips / Alms")
        ),
        CategoryDef(
            name = "Others",
            icon = "🌀",
            subcategories = listOf("Miscellaneous", "Cash Withdrawal Fees", "Lost Money")
        )
    )

    val incomeCategories = listOf(
        CategoryDef(
            name = "Salary",
            icon = "💵",
            subcategories = listOf("Primary Job Pay", "Overtime", "Bonus", "Commission", "Allowance")
        ),
        CategoryDef(
            name = "Freelancing",
            icon = "🚀",
            subcategories = listOf("Software Development", "Design", "Consulting", "Content Writing", "Video Editing")
        ),
        CategoryDef(
            name = "Business",
            icon = "🏢",
            subcategories = listOf("Product Sales", "Service Fees", "Affiliate Income", "Sponsorships")
        ),
        CategoryDef(
            name = "Rentals",
            icon = "🏠",
            subcategories = listOf("Residential Rent", "Commercial Lease", "Vehicle Rental")
        ),
        CategoryDef(
            name = "Gift",
            icon = "🎁",
            subcategories = listOf("Birthday Gift", "Festival Cash", "Inheritance")
        ),
        CategoryDef(
            name = "Refund",
            icon = "🔄",
            subcategories = listOf("Tax Refund", "Store Return Cashback", "Dispute Resolution")
        ),
        CategoryDef(
            name = "Investment Payouts",
            icon = "📈",
            subcategories = listOf("Stock Dividends", "Bond Yields", "Mutual Fund Capital Gains", "Fixed Deposit Interest", "Crypto Profits")
        ),
        CategoryDef(
            name = "Other Income",
            icon = "💰",
            subcategories = listOf("Credit Card Cashback", "Scrap Sale", "Miscellaneous Rewards")
        )
    )

    fun getSubcategoriesForCategory(category: String): List<String> {
        val expenseCat = expenseCategories.firstOrNull { it.name == category }
        if (expenseCat != null) return expenseCat.subcategories
        val incomeCat = incomeCategories.firstOrNull { it.name == category }
        if (incomeCat != null) return incomeCat.subcategories
        return emptyList()
    }

    fun getIconForCategory(category: String): String {
        val expenseCat = expenseCategories.firstOrNull { it.name == category }
        if (expenseCat != null) return expenseCat.icon
        val incomeCat = incomeCategories.firstOrNull { it.name == category }
        if (incomeCat != null) return incomeCat.icon
        return "💰"
    }
}
