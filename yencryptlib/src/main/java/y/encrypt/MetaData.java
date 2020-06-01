/*
 * This file is part of Y messenger.
 *
 * Y messenger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Y messenger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Y messenger.  If not, see <https://www.gnu.org/licenses/>.
 */

package y.encrypt;

import java.util.Date;

public class MetaData {

    long version;
    long meta;
    long type;
    long idEncryptKey;
    long idSignKey;

    public long getMeta() {
        return meta;
    }

    public void setMeta(long meta) {
        this.meta = meta;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }


    public long getIdEncryptKey() {
        return idEncryptKey;
    }

    public void setIdEncryptKey(long idEncryptKey) {
        this.idEncryptKey = idEncryptKey;
    }

    public long getIdSignKey() {
        return idSignKey;
    }

    public void setIdSignKey(long idSignKey) {
        this.idSignKey = idSignKey;
    }
}
