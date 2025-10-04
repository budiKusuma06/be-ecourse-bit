(ns be-ecourse-bit.domain.repositories.permission
  (:require [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(defprotocol PermissionRepository
  (find-all-permissions [this])
  (find-permission-by-code [this code])
  (find-permissions-by-ids [this ids])
  (get-admin-permissions [this admin-id])
  (grant-permission! [this admin-id permission-id granted-by])
  (revoke-permission! [this admin-id permission-id])
  (revoke-all-permissions! [this admin-id])
  (is-super-admin? [this admin-id])
  (has-permission? [this admin-id permission-code]))

(defrecord JdbcPermissionRepository [db]
  PermissionRepository
  (find-all-permissions [_]
    (sql/query db
               ["SELECT * FROM permissions ORDER BY category, name"]
               {:builder-fn rs/as-unqualified-lower-maps}))

  (find-permission-by-code [_ code]
    (first (sql/query db
                      ["SELECT * FROM permissions WHERE code = ?" code]
                      {:builder-fn rs/as-unqualified-lower-maps})))

  (find-permissions-by-ids [_ ids]
    (when (seq ids)
      (let [placeholders (clojure.string/join "," (repeat (count ids) "?"))]
        (sql/query db
                   (into [(str "SELECT * FROM permissions WHERE id IN (" placeholders ")")] ids)
                   {:builder-fn rs/as-unqualified-lower-maps}))))

  (get-admin-permissions [_ admin-id]
    (sql/query db
               ["SELECT p.* FROM permissions p
                 INNER JOIN admin_permissions ap ON p.id = ap.permission_id
                 WHERE ap.admin_id = ?" admin-id]
               {:builder-fn rs/as-unqualified-lower-maps}))

  (grant-permission! [_ admin-id permission-id granted-by]
    (sql/insert! db :admin_permissions
                 {:admin_id admin-id
                  :permission_id permission-id
                  :granted_by granted-by}))

  (revoke-permission! [_ admin-id permission-id]
    (jdbc/execute! db
                   ["DELETE FROM admin_permissions WHERE admin_id = ? AND permission_id = ?"
                    admin-id permission-id]))

  (revoke-all-permissions! [_ admin-id]
    (jdbc/execute! db
                   ["DELETE FROM admin_permissions WHERE admin_id = ?" admin-id]))

  (is-super-admin? [_ admin-id]
    (let [result (first (sql/query db
                                   ["SELECT is_super_admin FROM admin_profiles WHERE user_id = ?" admin-id]
                                   {:builder-fn rs/as-unqualified-lower-maps}))]
      (boolean (or (= 1 (:is_super_admin result))
                   (true? (:is_super_admin result))))))

  (has-permission? [this admin-id permission-code]
    (if (is-super-admin? this admin-id)
      true
      (let [result (first (sql/query db
                                     ["SELECT COUNT(*) as count FROM admin_permissions ap
                                       INNER JOIN permissions p ON ap.permission_id = p.id
                                       WHERE ap.admin_id = ? AND p.code = ?" admin-id permission-code]
                                     {:builder-fn rs/as-unqualified-lower-maps}))]
        (> (:count result) 0)))))

(defn create-repository [db]
  (->JdbcPermissionRepository db))