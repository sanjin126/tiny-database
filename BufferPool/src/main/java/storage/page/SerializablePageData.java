package storage.page;

import java.io.Serializable;

/**
 * 标识该类可以被序列化
 * 同时，该类可以在BasicPageGuard使用，用于在drop一个page时，将更新的内容反序列化
 * @see BasicPageGuard
 */
public interface SerializablePageData extends Serializable {
}
