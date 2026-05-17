package com.aditya.expent.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryUtils {

    // Massive dictionary of keywords structured for maximum performance O(1) keyword detection
    private val foodKeywords = setOf(
        "food", "dining", "restaurant", "cafe", "grocery", "groceries", "dinner", "lunch", 
        "coffee", "snack", "tea", "swiggy", "zomato", "sweet", "bakery", "drink", "bar", 
        "pub", "wine", "beer", "alcohol", "breakfast", "brunch", "pizza", "burger", "biryani", 
        "chicken", "dessert", "ice cream", "juice", "soda", "water", "chocolate", "fruit", 
        "vegetable", "milk", "bread", "cheese", "egg", "supermarket", "momos", "rolls", 
        "blinkit", "instamart", "zepto", "starbucks", "ccd", "cafe coffee day", "mcdonalds", 
        "mcd", "kfc", "burger king", "bk", "domino", "dominos", "pizza hut", "subway", 
        "haldiram", "haldirams", "saravana bhavan", "baristas", "blue tokai", "third wave", 
        "costco", "walmart", "target", "tesco", "lidl", "aldi", "grofers", "bigbasket", "dunzo"
    )

    private val subscriptionKeywords = setOf(
        "sub", "streaming", "netflix", "spotify", "prime", "hotstar", "youtube", "gym", "saas", 
        "software", "cloud", "apple", "google", "drive", "playstation plus", "ps plus", 
        "xbox game pass", "game pass", "nintendo", "crunchyroll", "duolingo", "medium", 
        "nytimes", "wsj", "linkedin premium", "zoom", "slack", "notion", "github", "figma", 
        "adobe", "canva", "microsoft 365", "office 365", "icloud", "dropbox", "gold's gym", 
        "cult fit", "cult.fit", "planet fitness", "anytime fitness", "membership", "license"
    )

    private val incomeKeywords = setOf(
        "salary", "job", "income", "bonus", "dividend", "cashback", "interest", "refund", 
        "freelance", "consulting", "paycheck", "stipend", "wages", "pocket money", "payout", 
        "earnings", "commission", "royalty", "profit", "grant", "scholarship", "gift card", 
        "rewards"
    )

    private val workKeywords = setOf(
        "work", "office", "laptop", "project", "client", "tool", "supply", "stationery", 
        "coworking", "desk", "monitor", "keyboard", "mouse", "printer", "postage", "shipping", 
        "conference", "seminar", "domain", "hosting", "ads", "marketing", "fb ads", 
        "google ads", "business travel"
    )

    private val shoppingKeywords = setOf(
        "shop", "buy", "apparel", "clothes", "clothing", "shoe", "footwear", "sneaker", 
        "shirt", "t-shirt", "tee", "jeans", "denim", "jacket", "suit", "dress", "skirt", 
        "pants", "trousers", "socks", "store", "mall", "gift", "purchase", "electronics", 
        "gadget", "phone", "smartph", "tablet", "ipad", "computer", "headph", "earbud", 
        "speaker", "watch", "smartw", "jewelry", "ring", "necklace", "makeup", "cosmetics", 
        "skincare", "perfume", "fragrance", "bag", "backpack", "wallet", "purse", "suitcase", 
        "luggage", "furniture", "decor", "home decor", "bed", "mattress", "pillow", "table", 
        "chair", "sofa", "appliance", "tv", "television", "fridge", "refrigerator", 
        "washing machine", "microwave", "oven", "ac", "air cond", "zara", "h&m", "handm", 
        "uniqlo", "marks & spencer", "m&s", "decathlon", "nike", "adidas", "puma", "reebok", 
        "under armour", "apple store", "samsung", "reliance digital", "croma", "vijay sales", 
        "ikea", "pepperfry", "urban ladder"
    )

    private val travelKeywords = setOf(
        "travel", "flight", "train", "bus", "metro", "taxi", "uber", "ola", "cab", "auto", 
        "fuel", "petrol", "diesel", "gas", "toll", "parking", "hotel", "stay", "booking", 
        "vacation", "trip", "holiday", "hostel", "resort", "ticket", "boarding", "transit", 
        "commuter", "commute", "car rental", "bike rental", "road trip", "rapido", "namma yatri", 
        "indrive", "makemytrip", "mmt", "yatra", "cleartrip", "goibibo", "booking.com", 
        "agoda", "airbnb", "hostelworld", "irctc", "indigo", "air india", "spicejet", 
        "akasa", "emirates", "qatar", "singapore airlines", "shell", "hp cl", "bpcl", "iocl"
    )

    private val billKeywords = setOf(
        "bill", "electricity", "water", "gas", "rent", "internet", "wifi", "broadband", 
        "fiber", "phone", "recharge", "mobile", "dth", "tv", "cable", "sewer", "garbage", 
        "trash", "waste", "maintenance", "society maintenance", "pg rent", "room rent", 
        "house rent", "lease", "bescom", "tneb", "msedcl", "upcl", "adani electricity", 
        "tata power", "bsnl", "jio", "airtel", "vi", "vodafone", "idea", "act fibernet", 
        "hathway", "tataplay", "dish tv"
    )

    private val leisureKeywords = setOf(
        "entertainment", "leisure", "play", "movie", "theater", "cinema", "concert", "show", 
        "event", "gig", "festival", "standup", "comedy", "game", "gaming", "playstation", 
        "steam", "xbox", "party", "nightout", "club", "pub", "bar", "disco", "zoo", "park", 
        "bowling", "arcade", "trampoline", "waterpark", "theme park", "amusement park", 
        "museum", "gallery", "exhibition", "activity", "sports", "match", "stadium", 
        "hobby", "craft", "bookmyshow", "bms", "pvr", "inox", "cinepolis", "ticketnew", 
        "epic games"
    )

    private val healthKeywords = setOf(
        "health", "medicine", "doctor", "clinic", "hospital", "pharmacy", "dental", "dentist", 
        "orthodontist", "eye clinic", "optician", "glasses", "lenses", "fitness", "workout", 
        "yoga", "pilates", "crossfit", "massage", "spa", "therapy", "therapist", "counselor", 
        "health insurance", "co-pay", "supplement", "protein", "vitamins", "treatment", 
        "healing", "care", "medical test", "lab test", "blood test", "1mg", "tata 1mg", 
        "pharmeasy", "apollo pharmacy", "medplus", "netmeds", "practo"
    )

    private val educationKeywords = setOf(
        "school", "college", "university", "tuition", "fees", "book", "textbook", "novel", 
        "comic", "ebook", "kindle", "course", "tutorial", "exam", "test", "certification", 
        "seminar", "training", "workshop", "boot camp", "coaching", "lecture", "class", 
        "udemy", "coursera", "edx", "khan academy", "skillshare", "masterclass"
    )

    private val investmentKeywords = setOf(
        "investment", "stock", "mutual fund", "fund", "crypto", "bitcoin", "ethereum", 
        "altcoin", "gold", "silver", "saving", "sip", "fd", "fixed deposit", "rd", 
        "recurring deposit", "deposit", "bond", "equity", "real estate", "land", "property", 
        "share", "trading", "broker", "demat", "portfolio", "groww", "zerodha", "coin", 
        "kuvera", "indmoney", "smallcase", "wazirx", "binance", "coinswitch"
    )

    private val familyKeywords = setOf(
        "family", "kid", "baby", "diapers", "toy", "parent", "dad", "mom", "brother", 
        "sister", "son", "daughter", "marriage", "wedding", "anniversary", "birthday", 
        "diwali", "holi", "christmas", "eid", "celebration", "donation", "charity", "ngo", 
        "present", "love", "heart", "valentine"
    )

    private val autoKeywords = setOf(
        "car wash", "car service", "bike service", "mechanic", "garage", "repair", 
        "spare parts", "tyre", "tire", "engine oil", "insurance", "rc", "license", 
        "registration", "helmet", "accessories", "auto care"
    )

    private val petKeywords = setOf(
        "pet", "dog", "cat", "puppy", "kitten", "vet", "veterinary", "pet food", 
        "pedigree", "whiskas", "pet grooming", "leash", "collar"
    )

    fun getCategoryIconAndColor(categoryName: String): Pair<ImageVector, Color> {
        val name = categoryName.lowercase().trim()

        // Match using premium fast sub-word search
        val icon = when {
            foodKeywords.any { name.contains(it) } -> Icons.Default.Restaurant
            subscriptionKeywords.any { name.contains(it) } -> Icons.Default.Subscriptions
            incomeKeywords.any { name.contains(it) } -> Icons.Default.BusinessCenter
            workKeywords.any { name.contains(it) } -> Icons.Default.Work
            shoppingKeywords.any { name.contains(it) } -> Icons.Default.ShoppingCart
            travelKeywords.any { name.contains(it) } -> Icons.Default.Flight
            billKeywords.any { name.contains(it) } -> Icons.Default.Receipt
            leisureKeywords.any { name.contains(it) } -> Icons.Default.LocalPlay
            healthKeywords.any { name.contains(it) } -> Icons.Default.Healing
            educationKeywords.any { name.contains(it) } -> Icons.Default.School
            investmentKeywords.any { name.contains(it) } -> Icons.AutoMirrored.Filled.TrendingUp
            familyKeywords.any { name.contains(it) } -> Icons.Default.Favorite
            autoKeywords.any { name.contains(it) } -> Icons.Default.Build
            petKeywords.any { name.contains(it) } -> Icons.Default.Pets
            else -> Icons.Default.Category
        }

        val color = when {
            // Food & Dining: Coral / Warm Orange
            foodKeywords.any { name.contains(it) } -> Color(0xFFFF7043)
            
            // Subscriptions: Sleek Purple
            subscriptionKeywords.any { name.contains(it) } -> Color(0xFFAB47BC)
            
            // Salary & Job: Emerald Green
            incomeKeywords.any { name.contains(it) } -> Color(0xFF009688)
            
            // Work & Office: Premium Blue
            workKeywords.any { name.contains(it) } -> Color(0xFF42A5F5)
            
            // Shopping: Neon Pink
            shoppingKeywords.any { name.contains(it) } -> Color(0xFFEC407A)
            
            // Travel: Deep Blue
            travelKeywords.any { name.contains(it) } -> Color(0xFF0288D1)
            
            // Bills: Teal / Aqua
            billKeywords.any { name.contains(it) } -> Color(0xFF26A69A)
            
            // Leisure: Gold / Yellow
            leisureKeywords.any { name.contains(it) } -> Color(0xFFFFCA28)
            
            // Health: Soft Red
            healthKeywords.any { name.contains(it) } -> Color(0xFFEF5350)
            
            // Education: Warm Brown
            educationKeywords.any { name.contains(it) } -> Color(0xFF8D6E63)
            
            // Investment: Lime Green
            investmentKeywords.any { name.contains(it) } -> Color(0xFF66BB6A)
            
            // Family & Personal: Magenta Pink
            familyKeywords.any { name.contains(it) } -> Color(0xFFD81B60)
            
            // Auto: Sleek Blue-Grey
            autoKeywords.any { name.contains(it) } -> Color(0xFF78909C)
            
            // Pets: Warm Sand / Clay
            petKeywords.any { name.contains(it) } -> Color(0xFF8D6E63)
            
            else -> Color(0xFF00B0FF) // Vibrant Cyan
        }

        return Pair(icon, color)
    }
}
