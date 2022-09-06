package ch.finecloud.peopledb.repository;

import ch.finecloud.peopledb.model.Entity;
import ch.finecloud.peopledb.model.Person;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

abstract class CRUDRepository<T extends Entity> {

    protected Connection connection;

    public CRUDRepository(Connection connection) {
        this.connection = connection;
    }

    public T save(T entity) {
        try {
            PreparedStatement ps = connection.prepareStatement(getSaveSql(), Statement.RETURN_GENERATED_KEYS);
            mapForSave(entity, ps);
            int recordsAffected = ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                long id = rs.getLong(1);
                entity.setId(id);
                System.out.println(entity);
            }
            System.out.printf("Records affected: %d%n", recordsAffected);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    public Optional<T> findById(Long id) {
        T entity = null;

        try {
            PreparedStatement ps = connection.prepareStatement(getFindByIdSql());
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entity = extractEntityFromResultSet(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.ofNullable(entity);
    }

    protected abstract String getFindAllSql();

    public List<T> findAll() {
        List<T> entities = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(getFindAllSql());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entities.add(extractEntityFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entities;
    }

    public long count() {
        long count = 0;
        try {
            PreparedStatement ps = connection.prepareStatement(getCountSql());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public void delete(T entity) {
        try {
            PreparedStatement ps = connection.prepareStatement(getDeleteSql());
            ps.setLong(1, entity.getId());
            int recordsAffected = ps.executeUpdate();
            System.out.println(recordsAffected);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(T...entities) {
        try {
            Statement stmt = connection.createStatement();
            String ids = Arrays.stream(entities).map(T::getId).map(String::valueOf).collect(Collectors.joining(","));
            int affectedRecordCount = stmt.executeUpdate(getDeleteInSql().replace(":ids", ids));
            System.out.println(affectedRecordCount);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(T entity) {
        try {
            PreparedStatement ps = connection.prepareStatement(getUpdateSql());
            mapForUpdate(entity, ps);
            ps.setLong(5, entity.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getUpdateSql();

    /**
     *
     * @return should return a SQL string like:
     * "DELETE FROM PEOPLE WHERE ID IN (:ids)"
     * Be sure to include the '(:ids)' named parameter & call it 'ids'
     */
    protected abstract String getDeleteInSql();

    protected abstract String getDeleteSql();

    protected abstract String getCountSql();

    abstract T extractEntityFromResultSet(ResultSet rs) throws SQLException;

    /**
     *
     * @return Returns a String that represents the SQL needed to retrive one entity.
     * The SQL must contain one SQL parameter, i.e. "?", that will bind to the
     * entity's ID.
     */
    protected abstract String getFindByIdSql();

    abstract void mapForSave(T entity, PreparedStatement ps) throws SQLException;
    abstract void mapForUpdate(T entity, PreparedStatement ps) throws SQLException;

    abstract String getSaveSql();

}
