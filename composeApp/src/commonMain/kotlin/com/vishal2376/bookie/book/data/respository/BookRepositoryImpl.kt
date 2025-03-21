package com.vishal2376.bookie.book.data.respository

import androidx.sqlite.SQLiteException
import com.vishal2376.bookie.book.data.database.FavoriteBookDao
import com.vishal2376.bookie.book.data.mapper.toBookEntity
import com.vishal2376.bookie.book.data.mapper.toDomain
import com.vishal2376.bookie.book.data.remote.BookDataSource
import com.vishal2376.bookie.book.domain.Book
import com.vishal2376.bookie.core.domain.DataError
import com.vishal2376.bookie.core.domain.EmptyResult
import com.vishal2376.bookie.core.domain.Result
import com.vishal2376.bookie.core.domain.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepositoryImpl(
	private val bookDataSource: BookDataSource,
	private val favoriteBookDao: FavoriteBookDao
) : BookRepository {
	override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
		return bookDataSource
			.searchBooks(query)
			.map { dto ->
				dto.results.map { it.toDomain() }
			}
	}

	override suspend fun getBookDescription(id: String): Result<String?, DataError.Remote> {
		val localResult = favoriteBookDao.getBookById(id)
		return if (localResult == null) {
			bookDataSource.getBookDetails(id).map { book ->
				book.description
			}
		} else {
			Result.Success(localResult.description)
		}
	}

	override fun getFavoriteBooks(): Flow<List<Book>> {
		return favoriteBookDao.getAllBooks()
			.map { bookEntities ->
				bookEntities.map { it.toDomain() }
			}
	}


	override fun isBookFavorite(id: String): Flow<Boolean> {
		return favoriteBookDao.getAllBooks()
			.map { bookEntities ->
				bookEntities.any { it.id == id }
			}
	}

	override suspend fun addFavoriteBook(book: Book): EmptyResult<DataError.Local> {
		return try {
			favoriteBookDao.upsertBook(book.toBookEntity())
			Result.Success(Unit)
		} catch (e: SQLiteException) {
			Result.Error(DataError.Local.DISK_FULL)
		}
	}

	override suspend fun deleteFavoriteBook(id: String) {
		favoriteBookDao.deleteBookById(id)
	}

}