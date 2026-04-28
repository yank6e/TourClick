package com.hfad.yultour

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.hfad.yultour.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем EdgeToEdge
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем статус-бар
        setupStatusBar()

        // Получаем NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Обработка системных отступов для BottomNavigation
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // ✅ ИСПРАВЛЕНО: используем ?: 0 для nullable Int
            val bottomPadding = systemBars.bottom ?: 0
            binding.bottomNavigation.setPadding(0, 0, 0, bottomPadding)

            insets
        }

        // Настраиваем Toolbar и навигацию
        setupToolbarAndNavigation()

        // Инициализируем данные Firebase
        initializeFirebaseData()

        // Проверяем авторизацию
        checkAuthStatus()
    }

    private fun setupStatusBar() {
        // Делаем статус-бар темно-зеленым как Toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.green_dark)
        }

        // Для Android M+ настраиваем светлый текст в статус-баре
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun setupToolbarAndNavigation() {
        // Устанавливаем Toolbar
        setSupportActionBar(binding.toolbar)

        // Настраиваем AppBarConfiguration
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.tourSearchFragment,
                R.id.favoritesFragment,
                R.id.purchasedFragment,
                R.id.profileFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // Настраиваем BottomNavigation
        binding.bottomNavigation.setupWithNavController(navController)

        // Слушатель для управления видимостью элементов
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.tourSearchFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = "YulTour"
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.favoritesFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = "Избранные"
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.purchasedFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = "Туры"
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.profileFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = getString(R.string.title_profile)
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.tourDetailsFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = "Детали тура"
                    binding.bottomNavigation.visibility = View.GONE
                }
                R.id.supportChatFragment -> {
                    supportActionBar?.show()
                    supportActionBar?.title = "Support Chat"
                    binding.bottomNavigation.visibility = View.GONE
                }
                R.id.loginFragment, R.id.registerFragment -> {
                    supportActionBar?.hide()
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    supportActionBar?.show()
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun initializeFirebaseData() {
        if (auth.currentUser != null) {
            val initializer = FirebaseDataInitializer(this)
            initializer.initializeToursData()
            initializer.initializeGuidesData()
            Log.d(TAG, "Firebase data initialized")
        }
    }

    private fun checkAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null && navController.currentDestination?.id != R.id.loginFragment) {
            // Направляем на логин если пользователь не авторизован
            navController.navigate(R.id.loginFragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun logout() {
        Log.d(TAG, "🚪 Logging out user: ${auth.currentUser?.email}")
        auth.signOut()
        // Перезапускаем активность для сброса навигации
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}