# Spring Boot - Build a REST CRUD API with JPA





### Create JPA DAO in Spring Boot

**Various DAO Techniques:**

- **Version 1:** Use EntityManager but leverage native Hibernate API
- **Version 2:** Use EntityManager and Standard JPA API
- **Version 3:** Spring Data JPA



**The Benefits of JPA:**

- By having a standard API, we are not locked to vendors implementation
- Maintain portable, flexible code
- Can theoretically switch vendor implementation
  - If Vendor ABC stops supporting their products
  - Switch to Vendor XYZ without vendor lock in



**Standard JPA API:**

- The JPA API methods are similar to native Hibernate Api

- JPA also supports a query language : JPQL(JPA Query Language)

  

**Comparing JPA to Native Hibernate Methods:**

| Action                    | Native Hibernate Method      | JPA Method                     |
| ------------------------- | ---------------------------- | ------------------------------ |
| Create/ save new entity   | session.save(...)            | entityManager.persist()        |
| Retrieve entity by id     | session.get(...) / load(...) | entityManager.find()           |
| Retrieve list of entities | session.createQuery(...)     | entityManager.createQuery(...) |
| Save or update entity     | session.saveOrUpdate(...)    | entityManage.merge(...)        |
| Delete entity             | session.delete(...)          | entityManager.remove(...)      |

High level comparison. Other options depending on context...





### **Version 2:** Use EntityManager and Standard JPA API

### Development Process

- **Step 1:** Setup Database Dev Environment
- **Step 2**:Create Spring Boot project using Spring Initializer
- **Step 3: **Get list of employees
- **Step 4**: Get single employee by ID
- **Step 5**: Add a new employee
- **Step 6: **Update an existing employee
- **Step 7: **Delete an existing employee



#### DAO Impl

> ```java
> @Repository
> public class EmployeeDAOJpaImpl implements EmployeeDAO {
>     
>     private EntityManager entityManager;
>     
>     @Autowired
>     public EmployeeDAOJpaImpl(EntityManager theEntityManager) {
>         entityManager = theEnityManager;
>     }
>     ....
> }
> ```



**Using Standard JPA API:**

1. **Get List of Employees**

```java
@override
public List<Employee> findAll() {
	// create a query
	TypedQuery<Employee> theQuery = entityManager.createQuery("from Employee", Employee.class);
    
    // execute the query and get result result List
    List<Employee>  employees = theQuery.getResultList();
    
    // return the results
    return employee;
}
```

No need to manage transactions. Handled at Service layer with **@Transactional**

```java
// if we use @Transactional annotaion we donnot use this code
transaction.begin();                   
someBusinessCode();                    
transaction.commit(); 
```

2. **Get a Single Employee**

   ```java
   @Override
   public Employee findById(int theId) {
   	
       // get employee
       Employee theEmployee = entityManager.find(Employee.class, theId);
       
       // return employee
       return theEmployee;
   }
   ```

3. **Add or Update Employee**

   ```java
   @Override
   public void save(Employee theEmployee) {
   	
       // save or update the employee
       // if id == 0, then save/insert else update
       Employee theEmployee = entityManager.merge(theEmployee);
       
       // update with id from db, so we can get generated id for save/insert
       // useful in our REST API to return generated id
       theEmployee.setId(theEmployee.getId());
       
   }
   ```

4. **Delete an existing employee**

   ```java
   @Override
   public void deleteById(int theId) {
   	
       // delete object with primary key
       Query theQuery = entityManager.createQuery("delete from Employee where id=:employeeId");
       theQuery.setParameter("employeeId", theId);
       
       theQuery.executeUpdate();
   }
   ```







### **Version 3:** Spring Data JPA

- We saw how to create a DAO for Employee

- What if we need to create a DAO for another entity?

  - Customer, Student, Product... etc

- Do we have to repeat all of the same code again?

  ![](https://githubpictures.000webhostapp.com/pictures/creating-dao.png)



**Solution: Spring Data JPA** 

- Create a DAO and just plug in your entity type and primary key
- Spring wil give us a CRUD implementation for free... like magic
  - Helps to minimize boiler-plate DAO code.



**JpaRepository**

- Spring Data JPA provides the interface: **JpaRepository**

- Expose methods (some by inheritance from parents)

  ![](https://githubpictures.000webhostapp.com/pictures/jpaRepository-methods.png)



**Development Process:**

- **Step 1:** Extend **JpaRepository** interface

- **Step 2:** Use Repository in the app

  

  **Step 1: Extend JpaRepository interface**

  ```java
  public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
  
  }
  ```

  ![](https://githubpictures.000webhostapp.com/pictures/jpa-repo.png)

  

  **Step 2: Use Repository in the app** 

  no need to use **@Transactional** since **JpaRepository** provides this functionally

  ```java
  @Service
  public class EmployeeServiceImpl implements EmployeeService {
  
      private EmployeeRepository employeeRepository;
      
      @Autowired
      public EmployeeServiceImpl (EmployeeRepository 			  											theEmployeeRepository){
          
          employeeRepository = theEmployeeRepository
      }
  }
  ```

  ![](https://githubpictures.000webhostapp.com/pictures/repository-in-serviceImpl.png)





**Advanced Features**

- Advanced features available for 
  - Extending and adding custom queries with JPQL
  - Query Domain Specific Language (Query DSL)
  - Defining custom methods (low-level coding)







# { The End }