package com.excilys.formation.persistence.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import com.excilys.formation.exception.PersistenceException;
import com.excilys.formation.mapper.JdbcMapper;
import com.excilys.formation.model.Computer;
import com.excilys.formation.model.util.PageFilter;
import com.excilys.formation.pagination.Page;
import com.excilys.formation.persistence.ComputerDao;
import com.excilys.formation.persistence.HikariConnectionProvider;
import com.excilys.formation.util.DateConverter;
import ch.qos.logback.classic.Logger;

/**
 * DAO class for computers.
 * @author kfuster
 *
 */
public class ComputerDaoJdbc implements ComputerDao {
    final Logger logger = (Logger) LoggerFactory.getLogger(ComputerDaoJdbc.class);
    private HikariConnectionProvider connectionProvider;
    private static ComputerDaoJdbc computerDaoImpl = null;
    private static final String INSERT_COMPUTER = "INSERT INTO computer(name, introduced, discontinued, company_id) VALUES(?, ?, ?, ?)";
    private static final String UPDATE_COMPUTER = "UPDATE computer SET name=?, introduced=?, discontinued=?, company_id=? WHERE id=?";
    private static final String DELETE_COMPUTER = "DELETE FROM computer WHERE id=?";
    private static final String DELETE_COMPUTER_BY_COMPANY = "DELETE FROM computer WHERE company_id=?";
    private static final String SELECT_JOIN = "SELECT computer.id, computer.name, computer.introduced, computer.discontinued, company.id as companyId, company.name as companyName FROM computer LEFT JOIN company ON computer.company_id=company.id";
    private static final String COUNT_ALL = "SELECT COUNT(*) as total FROM computer LEFT JOIN company ON computer.company_id=company.id ";
    /**
     * Constructor for ComputerDaoJdbc. Initializes the connectionProvider.
     */
    private ComputerDaoJdbc() {
        connectionProvider = HikariConnectionProvider.getInstance();
    }
    /**
     * Getter for the ComputerDaoJdbc instance. Initializes it if null.
     * @return the instance of ComputerDaoJdbc
     */
    public static ComputerDaoJdbc getInstance() {
        if (computerDaoImpl == null) {
            computerDaoImpl = new ComputerDaoJdbc();
        }
        return computerDaoImpl;
    }
    @Override
    public Computer create(Computer pComputer) throws PersistenceException {
        String queryComputer = INSERT_COMPUTER;
        try (Connection connection = connectionProvider.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryComputer,
                    PreparedStatement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, pComputer.getName());
            preparedStatement.setTimestamp(2, DateConverter.fromLocalDateToTimestamp(pComputer.getIntroduced()));
            preparedStatement.setTimestamp(3, DateConverter.fromLocalDateToTimestamp(pComputer.getDiscontinued()));
            preparedStatement.setLong(4, pComputer.getCompany().getId());
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet != null && resultSet.next()) {
                int computerId = resultSet.getInt(1);
                pComputer.setId(computerId);
                connection.close();
                return pComputer;
            }
        } catch (SQLException e) {
            logger.error("Error in ComputerDao : create : ", e);
            throw new PersistenceException("Problème lors de la création de l'ordinateur");
        }
        return null;
    }
    @Override
    public void update(Computer pComputer) throws PersistenceException {
        String queryComputer = UPDATE_COMPUTER;
        try (Connection connection = connectionProvider.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryComputer);
            preparedStatement.setString(1, pComputer.getName());
            preparedStatement.setTimestamp(2, DateConverter.fromLocalDateToTimestamp(pComputer.getIntroduced()));
            preparedStatement.setTimestamp(3, DateConverter.fromLocalDateToTimestamp(pComputer.getDiscontinued()));
            preparedStatement.setLong(4, pComputer.getCompany().getId());
            preparedStatement.setLong(5, pComputer.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error in ComputerDao : update : ", e);
            throw new PersistenceException("Problème lors de l'update de l'ordinateur");
        }
    }
    @Override
    public boolean delete(long pID) throws PersistenceException {
        try (Connection connection = connectionProvider.getConnection()) {
            int affectedRow = 0;
            try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_COMPUTER)) {
                preparedStatement.setLong(1, pID);
                affectedRow = preparedStatement.executeUpdate();
            } catch (SQLException e) {
                logger.error("Error in ComputerDao : delete : ", e);
                throw new PersistenceException("Problème lors de la suppression de l'ordinateur");
            }
            if (affectedRow == 1) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error in ComputerDao : delete : ", e);
            throw new PersistenceException("Problème lors de la suppression de l'ordinateur");
        }
    }
    @Override
    public boolean deleteByCompany(long pID, Connection pConnection) throws PersistenceException {
        try {
            int affectedRow = 0;
            try (PreparedStatement preparedStatement = pConnection.prepareStatement(DELETE_COMPUTER_BY_COMPANY)) {
                preparedStatement.setLong(1, pID);
                affectedRow = preparedStatement.executeUpdate();
            } catch (SQLException e) {
                pConnection.rollback();
                logger.error("Error in ComputerDao : deleteByCompany : ", e);
                throw new PersistenceException("Problème lors de la suppression des ordinateur");
            }
            if (affectedRow >= 1) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error in ComputerDao : deleteByCompany : ", e);
            throw new PersistenceException("Problème lors de la suppression des ordinateur");
        }
    }
    @Override
    public Computer getById(long pId) throws PersistenceException {
        String queryComputer = SELECT_JOIN + " WHERE computer.id=?";
        Computer computer = null;
        try (Connection connection = connectionProvider.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(queryComputer);
            ps.setLong(1, pId);
            ResultSet resultSet = ps.executeQuery();
            computer = JdbcMapper.mapResultToComputer(resultSet);
        } catch (SQLException e) {
            logger.error("Error in ComputerDao : getById : ", e);
            throw new PersistenceException("Problème lors de la récupération de l'ordinateur");
        }
        return computer;
    }
    @Override
    public Computer getByName(String pName) throws PersistenceException {
        String queryComputer = SELECT_JOIN + " WHERE computer.name=?";
        Computer computer = null;
        try (Connection connection = connectionProvider.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryComputer);
            preparedStatement.setString(1, pName);
            ResultSet resultSet = preparedStatement.executeQuery();
            computer = JdbcMapper.mapResultToComputer(resultSet);
        } catch (SQLException e) {
            logger.error("Error in ComputerDao : getByName : ", e);
            throw new PersistenceException("Problème lors de la récupération de l'ordinateur");
        }
        return computer;
    }
    @Override
    public Page<Computer> getPage(PageFilter pPageFilter) throws PersistenceException {
        List<Computer> computers = new ArrayList<>();
        String queryComputers = SELECT_JOIN;
        String conditions = "";
        if (pPageFilter.getConditions() != null && !pPageFilter.getConditions().isEmpty()) {
            conditions += mapConditions(pPageFilter.getConditions());
        }
        queryComputers += conditions + " LIMIT ? OFFSET ?";
        Page<Computer> pPage = new Page<>(pPageFilter.getElementsByPage());
        try (Connection connection = connectionProvider.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(queryComputers);
            preparedStatement.setInt(1, pPageFilter.getElementsByPage());
            preparedStatement.setInt(2, (pPageFilter.getPageNum() - 1) * pPageFilter.getElementsByPage());
            ResultSet resultSet = preparedStatement.executeQuery();
            computers = JdbcMapper.mapResultsToComputerList(resultSet);
            pPage.page = pPageFilter.getPageNum();
            pPage.elems = computers;
            pPage.setTotalElement(count(conditions));
            pPageFilter.setNbPage(pPage.nbPages);
        } catch (SQLException e) {
            logger.error("Error in ComputerDao : getPage : ", e);
            throw new PersistenceException("Problème lors de la récupération de la page d'ordinateurs");
        }
        return pPage;
    }
    private String mapConditions(Map<String, String> pConditions) {
        String conditions = "";
        if(pConditions != null && !pConditions.isEmpty()) {
            conditions = " WHERE";
            if (pConditions.containsKey("computerName")) {
                conditions += " computer.name like '%" + pConditions.get("computerName") + "%'";
            }
            if (pConditions.containsKey("operator")) {
                conditions += " " + pConditions.get("operator");
            }
            else {
                conditions += "OR ";
            }
            if (pConditions.containsKey("companyName")) {
                conditions += " company.name like '%" + pConditions.get("companyName") + "%'";
            }
        }
        return conditions;
    }
    /**
     * Count the number of Computers in the DB.
     * @param pFilter an optional filter string
     * @return the number of computers in the DB
     */
    private int count(String pFilter) {
        String query = COUNT_ALL;
        if (pFilter != null && !pFilter.isEmpty()) {
            query += pFilter;
        }
        int total = 0;
        try (Connection connection = connectionProvider.getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            resultSet.next();
            total = resultSet.getInt("total");
        } catch (SQLException e) {
            logger.error("Error in ComputerDao : count : ", e);
        }
        return total;
    }
}