package com.example.petquest.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.petquest.data.model.AchievementEntity;
import com.example.petquest.data.model.PetEntity;
import com.example.petquest.data.model.PetType;
import com.example.petquest.data.model.TaskEntity;
import com.example.petquest.data.model.TaskType;
import com.example.petquest.data.model.Virtue;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PetQuestDao_Impl implements PetQuestDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PetEntity> __insertionAdapterOfPetEntity;

  private final Converters __converters = new Converters();

  private final EntityInsertionAdapter<TaskEntity> __insertionAdapterOfTaskEntity;

  private final EntityInsertionAdapter<AchievementEntity> __insertionAdapterOfAchievementEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateBondPoints;

  private final SharedSQLiteStatement __preparedStmtOfVerifyPet;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePet;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePetPhotoUri;

  private final SharedSQLiteStatement __preparedStmtOfDeletePetById;

  private final SharedSQLiteStatement __preparedStmtOfCompleteTask;

  private final SharedSQLiteStatement __preparedStmtOfClearAllTasks;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTasksForPet;

  private final SharedSQLiteStatement __preparedStmtOfUnlockAchievement;

  public PetQuestDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPetEntity = new EntityInsertionAdapter<PetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pets` (`id`,`name`,`type`,`virtue`,`bondPoints`,`bondLevel`,`isVerified`,`photoUri`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PetEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        final String _tmp = __converters.fromPetType(entity.getType());
        statement.bindString(3, _tmp);
        final String _tmp_1 = __converters.fromVirtue(entity.getVirtue());
        statement.bindString(4, _tmp_1);
        statement.bindLong(5, entity.getBondPoints());
        statement.bindLong(6, entity.getBondLevel());
        final int _tmp_2 = entity.isVerified() ? 1 : 0;
        statement.bindLong(7, _tmp_2);
        if (entity.getPhotoUri() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getPhotoUri());
        }
      }
    };
    this.__insertionAdapterOfTaskEntity = new EntityInsertionAdapter<TaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tasks` (`id`,`petId`,`title`,`type`,`isCompleted`,`date`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPetId());
        statement.bindString(3, entity.getTitle());
        final String _tmp = __converters.fromTaskType(entity.getType());
        statement.bindString(4, _tmp);
        final int _tmp_1 = entity.isCompleted() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
        statement.bindString(6, entity.getDate());
      }
    };
    this.__insertionAdapterOfAchievementEntity = new EntityInsertionAdapter<AchievementEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `achievements` (`id`,`title`,`description`,`isUnlocked`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AchievementEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getDescription());
        final int _tmp = entity.isUnlocked() ? 1 : 0;
        statement.bindLong(4, _tmp);
      }
    };
    this.__preparedStmtOfUpdateBondPoints = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pets SET bondPoints = ?, bondLevel = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfVerifyPet = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pets SET isVerified = 1, photoUri = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdatePet = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pets SET name = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdatePetPhotoUri = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pets SET photoUri = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeletePetById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pets WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfCompleteTask = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE tasks SET isCompleted = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllTasks = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tasks";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteTasksForPet = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tasks WHERE petId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUnlockAchievement = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE achievements SET isUnlocked = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPet(final PetEntity pet, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPetEntity.insert(pet);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTask(final TaskEntity task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTaskEntity.insert(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAchievement(final AchievementEntity achievement,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAchievementEntity.insert(achievement);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBondPoints(final int petId, final int points, final int level,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateBondPoints.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, points);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, level);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, petId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateBondPoints.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object verifyPet(final int petId, final String uri,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfVerifyPet.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, uri);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, petId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfVerifyPet.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePet(final int petId, final String name,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePet.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, name);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, petId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdatePet.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePetPhotoUri(final int petId, final String newUri,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePetPhotoUri.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, newUri);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, petId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdatePetPhotoUri.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePetById(final int petId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePetById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, petId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeletePetById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object completeTask(final int taskId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCompleteTask.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, taskId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfCompleteTask.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllTasks(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllTasks.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAllTasks.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTasksForPet(final int petId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTasksForPet.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, petId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteTasksForPet.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object unlockAchievement(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUnlockAchievement.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUnlockAchievement.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PetEntity>> getAllPets() {
    final String _sql = "SELECT * FROM pets ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pets"}, new Callable<List<PetEntity>>() {
      @Override
      @NonNull
      public List<PetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfVirtue = CursorUtil.getColumnIndexOrThrow(_cursor, "virtue");
          final int _cursorIndexOfBondPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "bondPoints");
          final int _cursorIndexOfBondLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "bondLevel");
          final int _cursorIndexOfIsVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "isVerified");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUri");
          final List<PetEntity> _result = new ArrayList<PetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PetEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final PetType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toPetType(_tmp);
            final Virtue _tmpVirtue;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfVirtue);
            _tmpVirtue = __converters.toVirtue(_tmp_1);
            final int _tmpBondPoints;
            _tmpBondPoints = _cursor.getInt(_cursorIndexOfBondPoints);
            final int _tmpBondLevel;
            _tmpBondLevel = _cursor.getInt(_cursorIndexOfBondLevel);
            final boolean _tmpIsVerified;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsVerified);
            _tmpIsVerified = _tmp_2 != 0;
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            _item = new PetEntity(_tmpId,_tmpName,_tmpType,_tmpVirtue,_tmpBondPoints,_tmpBondLevel,_tmpIsVerified,_tmpPhotoUri);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TaskEntity>> getTodaysTasks(final String date) {
    final String _sql = "SELECT * FROM tasks WHERE date = ? ORDER BY type ASC, id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPetId = CursorUtil.getColumnIndexOrThrow(_cursor, "petId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpPetId;
            _tmpPetId = _cursor.getInt(_cursorIndexOfPetId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final TaskType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toTaskType(_tmp);
            final boolean _tmpIsCompleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp_1 != 0;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            _item = new TaskEntity(_tmpId,_tmpPetId,_tmpTitle,_tmpType,_tmpIsCompleted,_tmpDate);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTaskCountForPet(final int petId, final String date,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM tasks WHERE petId = ? AND date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, petId);
    _argIndex = 2;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AchievementEntity>> getAllAchievements() {
    final String _sql = "SELECT * FROM achievements ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"achievements"}, new Callable<List<AchievementEntity>>() {
      @Override
      @NonNull
      public List<AchievementEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsUnlocked = CursorUtil.getColumnIndexOrThrow(_cursor, "isUnlocked");
          final List<AchievementEntity> _result = new ArrayList<AchievementEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AchievementEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsUnlocked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsUnlocked);
            _tmpIsUnlocked = _tmp != 0;
            _item = new AchievementEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpIsUnlocked);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAchievementCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM achievements";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
