package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle

// Profile sections read as one continuous document rather than floating cards.
internal val ProfileCardCornerRadius = 0.dp
internal val ProfileCardShape = RoundedCornerShape(ProfileCardCornerRadius)

internal fun profileCardBackdropShape() = RoundedRectangle(ProfileCardCornerRadius)
