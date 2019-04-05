package org.springframework.jdbc.core;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.Assert;

/**
 * Based on the Spring Data Pageable API, this extension adds a {@link #queryForPage} 
 * method to the {@link JdbcTemplate} which through SQL subselect wrapping techniques 
 * enables the user to query for paged (and sorted) results.
 * 
 * @author David Mališ
 * @since March 4, 2019
 * 
 * @see #queryForPage(String, Object[], Pageable, RowMapper)
 * @see PagingDialect
 * @see PageResultSetExtractor
 */
public class PagingAndSortingJdbcTemplate extends JdbcTemplate {
	
	/**
	 * Dialect used to paginate results.
	 */
    private PagingDialect pagingDialect;
    
    /**
     * Constructs a new instance with the specified {@link PagingDialect} used 
     * to apply pagination to SQL queries. {@link DataSource} is supplied to 
     * the underlying {@link JdbcTemplate}.
     * 
     * @param datasource for the {@link JdbcTemplate} to obtain connections from
     * @param pagingDialect used to apply pagination to SQL queries
     * @see JdbcTemplate#JdbcTemplate(DataSource)
     */
    public PagingAndSortingJdbcTemplate(DataSource datasource,
        PagingDialect pagingDialect) {
    	this(datasource, pagingDialect, true);
    }
    
    /**
     * Constructs a new instance with the specified {@link PagingDialect} used 
     * to apply pagination to SQL queries. {@link DataSource} and 'lazyInit'
     * flag are supplied to the underlying {@link JdbcTemplate}.
     * 
     * @param datasource for the {@link JdbcTemplate} to obtain connections from
     * @param pagingDialect used to apply pagination to SQL queries
     * @param lazyInit whether to lazily initialize the SQLExceptionTranslator
     * @see JdbcTemplate#JdbcTemplate(DataSource, boolean)
     */
    public PagingAndSortingJdbcTemplate(DataSource datasource,
        PagingDialect pagingDialect, boolean lazyInit) {

        super(datasource, lazyInit);
        Assert.notNull(pagingDialect, "PagingDialect must not be null");
        this.pagingDialect = pagingDialect;
    }
    
    /**
     * <b>In most cases one of the {@link PagingDialects} should be used.</b>
     * <p>
     * This interface enables developers to apply their own pagination
     * mechanism to the SQL queries. Implementations should apply pagination 
     * mechanism to the supplied SQL query in such a way that each row of the 
     * {@link ResultSet} contains information of its position relative to the 
     * total number of rows.
     * </p><p>
     * <b>Example:</b>
     * 
     * <table>
     * 	<thead>
     * 	<tr>
     * 		<th>PART</th>
     * 		<th>TOTAL</th>
     * 		<th>COLUMN 1</th>
     * 		<th>COLUMN 2</th>
     * 	<tr>
     * 	</thead>
     * 	<tbody>
     * 	<tr>
     * 		<td>1</td>
     * 		<td>3</td>
     * 		<td>data 1</td>
     * 		<td>data 2</td>
     * 	</tr>
     * 	<tr>
     * 		<td>2</td>
     * 		<td>3</td>
     * 		<td>data 3</td>
     * 		<td>data 4</td>
     * 	</tr>
     * 	<tr>
     * 		<td>3</td>
     * 		<td>3</td>
     * 		<td>data 5</td>
     * 		<td>data 6</td>
     * 	</tr>
     * 	</tbody>
     * </table>
     * <p>
     * Each row should have a "total" column with the total number of rows.
     * 
     * <p><b>See {@link PagingDialects} for the examples.</b></p>
     * @see PagingDialects 
     * @see PageResultSetExtractor
     */
    public interface PagingDialect {
   
    	/**
    	 * Should apply paging mechanism to the supplied SQL query based
    	 * on the specified {@link Pageable}.
    	 * 
    	 * @see PagingDialect
    	 * @see PagingDialects
    	 * @see PageResultSetExtractor
    	 * 
    	 * @param sql to which paging mechanism is applied
    	 * @param pageable containing paging parameters
    	 * @return {@link String} paginated SQL query
    	 */
    	public String applyPageable(String sql, Pageable pageable);
    	
    }
    
    /**
     * Default implementations of the {@link PagingDialect} interface.
     * 
     * @see PagingDialect
     * @see PageResultSetExtractor
     */
    public enum PagingDialects implements PagingDialect {

        DB2 		("select * from (select row_number() over() as part, count(1) over() total, q.* from ( %s ) q) p where part between %d and %d"),
        ORACLE 		("select * from (select rownum part, count(1) over() total, q.* from ( %s ) q) where part between %d and %d"),
        SQL_SERVER 	("select * from (select row_number() over(order by 1/0) part, count(1) over() total, q.* from ( %s ) q) p where part between %d and %d"), 
        MYSQL 		("select * from (select (@r:=@r+1) as part, (select count(1) from q) as total, q.* from ( %s ) as q, (select @r:=0) as t) as p where part between %d and %d");
    	
    	//TODO H2 support

        private String format;
        private PagingDialects(String format) {
            this.format = format;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.jdbc.core.PagingAndSortingJdbcTemplate.PagingDialect#applyPageable
         */
        @Override
        public String applyPageable(String sql, Pageable pageable) {

        	Sort sort = pageable.getSort();
        	if(sort != null && sort.isSorted()) {
    	        final Iterator<Order> it = sort.iterator();
    	        final StringBuilder sb = 
    	        	new StringBuilder("select * from ( ")
    	        		.append(sql).append(" ) o order by ");
    	
    	        while(it.hasNext()) {
    	        	Order o = it.next();
    	            sb.append(o.getProperty()).append(" ").append(o.getDirection());
    	            if(it.hasNext()) {
    	            	sb.append(", ");
    	            }
    	        }
    	        
    	        if(this == PagingDialects.SQL_SERVER) {
    	        	sb.append(" offset 0 rows");
    	        }
    	        
    	        sql = sb.toString();
        	}
        	        
            int pageSize = pageable.getPageSize();
            long offset = pageable.getOffset();

            return String.format(format, sql, offset+1, offset+pageSize);
            
        }
    }

    /**
     * {@link ResultSetExtractor} implementation which is used to extract
     * {@link Page}s from the {@link ResultSet} of an execution of a paginated
     * SQL query. Pagination mechanism are applied in the {@link PagingDialect}
     * implementation.
     * 
     * @param <T> model object type
     * @see PagingDialect
     * @see PagingDialects
     */
    private class PageResultSetExtractor<T> 
    	implements ResultSetExtractor<PageImpl<T>> {

        private Pageable pageable;

        private RowMapper<T> rowMapper;

        /**
         * Constructs a {@link PageResultSetExtractor} instance with the given {@link Pageable}
         * and {@link RowMapper}.
         * 
         * @param pageable containing original paging parameters
         * @param rowMapper used to map rows to model objects
         */
        private PageResultSetExtractor(Pageable pageable, RowMapper<T> rowMapper) {
            this.rowMapper = rowMapper;
            this.pageable = pageable;
        }

        /**
         * Extracts a {@link ResultSet} into a {@link PageImpl} instance containing 
         * a total number of rows expected in the "total" column of the first row. 
         * If {@link ResultSet} contains no rows, extraction will result in an empty 
         * {@link PageImpl}.
         * 
         * @param rs is the result set which is being extracted
         * @return {@link PageImpl} instance
         */
        @Override
        public PageImpl<T> extractData(ResultSet rs) throws SQLException {
            final List<T> content = new ArrayList<>();
            long total = 0;
            int rowNum = 0;
            while(rs.next()) {
                if(rowNum == 0) {
                    total = rs.getLong("total");
                }
                content.add(rowMapper.mapRow(rs, rowNum++));
            }
            return new PageImpl<>(content, pageable, total);
        }
    }


    /**
     * Applies {@link Pageable} to the specified SQL and executes it with the given parameters. 
     * Later it uses specified {@link RowMapper} to retrieve model object instances into a 
     * {@link PageImpl} instance which represents a single page providing total number of rows, 
     * partial paginated result and the original {@link Pageable}.
     *
     * @param <T> model object type
     * @param sql to which pagination mechanism is applied
     * @param params for the sql execution
     * @param pageable which contains pagination parameters
     * @param rowMapper which maps model objects <T> from the result set
     * @return Page<T> of model objects
     * @see PageResultSetExtractor
     * @see PagingDialect
     * @see Page
     * @see PageImpl
     * @see Pageable
     * @see RowMapper
     * @see JdbcTemplate#query(String, Object[], ResultSetExtractor)
     */
    public <T> Page<T> queryForPage(String sql, Object[] params,
            Pageable pageable, RowMapper<T> rowMapper){
    	
    	Assert.notNull(sql, "Sql must not be null");
    	Assert.notNull(pageable, "Pageable must not be null");
    	Assert.notNull(rowMapper, "RowMapper must not be null");

        String paginatedSql = pagingDialect.applyPageable(sql, pageable);
        return query(paginatedSql, params, new PageResultSetExtractor<>(pageable, rowMapper));
    }
    
}