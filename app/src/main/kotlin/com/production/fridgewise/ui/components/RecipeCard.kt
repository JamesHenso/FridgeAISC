package com.production.fridgewise.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.production.fridgewise.ui.theme.MyAppTheme

data class RecipeCardUi(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val category: String?,
    val servings: Int?,
    val time: Int?,
    val difficulty: String?,
    val likeCount: Int?,
    val isFavorite: Boolean = false
)

fun com.production.fridgewise.data.remote.dto.RecipeCardResponse.toUi(isFavorite: Boolean = false) = RecipeCardUi(
    id = id, name = name, imageUrl = imageUrl, category = category,
    servings = servings, time = time, difficulty = difficulty,
    likeCount = like_count, isFavorite = isFavorite
)

@Composable
fun RecipeCard(
    recipe: RecipeCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteClick: (() -> Unit)? = null
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            AsyncImage(
                model = recipe.imageUrl,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(recipe.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(
                            recipe.category,
                            recipe.time?.let { "${it}p" },
                            recipe.difficulty,
                            recipe.likeCount?.let { "♥ $it" }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (onFavoriteClick != null) {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "favorite"
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeCardPreview() {
    MyAppTheme {
        RecipeCard(
            RecipeCardUi("r1", "Cơm chiên", null, "Món mặn", 2, 30, "Dễ", 12, false),
            onClick = {}, onFavoriteClick = {}
        )
    }
}
