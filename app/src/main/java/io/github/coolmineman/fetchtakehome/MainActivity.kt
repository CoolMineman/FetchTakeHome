package io.github.coolmineman.fetchtakehome

import android.os.Bundle
import android.util.JsonReader
import android.util.JsonToken
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.coolmineman.fetchtakehome.ui.theme.FetchTakeHomeTheme
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.TreeMap

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FetchTakeHomeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Thread {
            var items: TreeMap<Int, ArrayList<ListItem>>
            while (true) {
                try {
                    URL("https://fetch-hiring.s3.amazonaws.com/hiring.json").openStream().use {
                        items = parseItems(JsonReader(BufferedReader(InputStreamReader(it))))
                    }
                } catch (e: Exception) {
                    var message = "Unknown Error. Retrying..."
                    if (e is IOException) message =
                        "Unable to download data from server. Retrying..."
                    e.printStackTrace()
                    runOnUiThread {
                        setContent {
                            FetchTakeHomeTheme {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Thread.sleep(5000)
                    continue
                }
                break
            }

            runOnUiThread {
                setContent {
                    FetchTakeHomeTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                            ) {
                                for (e in items) {
                                    Group(listId = e.key, listItems = e.value)
                                }
                            }
                        }
                    }
                }
            }
        }.start()

    }
}

fun parseItems(reader: JsonReader): TreeMap<Int, ArrayList<ListItem>> {
    val r = TreeMap<Int, ArrayList<ListItem>>()
    reader.beginArray()
    while (reader.hasNext()) {
        val item = parseItem(reader)
        if (!item.item.name.isNullOrBlank()) {
            (r.computeIfAbsent(item.listId) { ArrayList() }).add(item.item)
        }
    }
    for (itemList in r.values) {
        itemList.sortWith { a, b -> a.name!!.compareTo(b.name!!) }
    }
    reader.endArray()
    return r
}

fun parseItem(reader: JsonReader): GroupedListItem {
    var id: Int? = null
    var listId: Int? = null
    var name: String? = null
    reader.beginObject()
    while (reader.hasNext()) {
        when (reader.nextName()) {
            "id" -> id = reader.nextInt()
            "listId" -> listId = reader.nextInt()
            "name" -> name = if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();null
            } else reader.nextString()

            else -> reader.skipValue()
        }
    }
    reader.endObject()
    if (id == null) throw RuntimeException("Item missing id")
    if (listId == null) throw RuntimeException("Item missing listId")
    return GroupedListItem(listId, ListItem(id, name))
}

data class GroupedListItem(val listId: Int, val item: ListItem)
data class ListItem(val id: Int, val name: String?)

@Composable
fun Group(listId: Int, listItems: List<ListItem>) {
    Column {
        Row(modifier = Modifier.padding(all = 8.dp)) {
            Text(
                text = "List $listId",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)
        }

        Spacer(modifier = Modifier.height(3.dp))

        for (item in listItems) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 1.dp)
                    .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "• ${item.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "(Id: " + "${item.id}".padStart(3, ' ') + ")",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun Preview() {
    FetchTakeHomeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                repeat(10) {
                    Group(1, listOf(ListItem(5, "Yeet5"), ListItem(6, "Yeet6")))
                    Group(2, listOf(ListItem(7, "Yeet7"), ListItem(843, "Yeetbutlonger8")))
                }
            }
        }
    }
}