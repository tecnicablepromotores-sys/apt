package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ===== Tema claro (calcado de body.light-theme en index.html) =====
val TecnicableBackground = Color(0xFFF0F4F8)      // --bg-body (light)
val TecnicableSurface = Color(0xFFFFFFFF)         // --bg-card (light), sólido para Compose
val TecnicableSurfaceVariant = Color(0xFFE7ECF2)  // tarjetas secundarias / inputs
val TecnicablePrimary = Color(0xFF0D9488)         // --accent (light) = teal, IGUAL que la web
val TecnicablePrimaryHover = Color(0xFF0F766E)    // --accent-hover (light)
val TecnicableOnPrimary = Color(0xFFFFFFFF)
val TecnicableOnBackground = Color(0xFF1E293B)    // --text-primary (light)
val TecnicableTextSecondary = Color(0xFF475569)   // --text-secondary (light)
val TecnicableTextMuted = Color(0xFF94A3B8)       // --text-muted (light)
val TecnicableBorder = Color(0x14000000)          // --border-color (light) ~ rgba(0,0,0,.08)

// ===== Tema oscuro (calcado de :root en index.html) =====
val TecnicableDarkBackground = Color(0xFF0D0D12)     // --bg-body (dark)
val TecnicableDarkSurface = Color(0xFF18181C)        // --bg-card (dark), sólido
val TecnicableDarkSurfaceVariant = Color(0xFF111116) // --bg-sidebar/--bg-header (dark)
val TecnicableDarkPrimary = Color(0xFF3B82F6)        // --accent (dark) = azul, sin cambios
val TecnicableDarkPrimaryHover = Color(0xFF2563EB)   // --accent-hover (dark)
val TecnicableDarkOnPrimary = Color(0xFFFFFFFF)
val TecnicableDarkOnBackground = Color(0xFFE2E8F0)   // --text-primary (dark)
val TecnicableDarkTextSecondary = Color(0xFF9CA3AF)  // --text-secondary (dark)
val TecnicableDarkTextMuted = Color(0xFF4B4B55)      // --text-muted (dark)
val TecnicableDarkBorder = Color(0x0FFFFFFF)         // --border-color (dark) ~ rgba(255,255,255,.06)

// ===== Acento secundario "estado" (el mismo violeta que ya usabas en StatusFilters/EmptyState,
// formalizado aquí porque coincide con --accent morado de .btn-action.estado en la web) =====
val TecnicableStatusAccent = Color(0xFF8B5CF6)
