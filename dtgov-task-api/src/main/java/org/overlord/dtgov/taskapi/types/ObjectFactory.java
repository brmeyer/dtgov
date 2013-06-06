//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.06.06 at 03:05:40 PM EDT 
//


package org.overlord.dtgov.taskapi.types;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.overlord.dtgov.taskapi.types package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Task_QNAME = new QName("http://downloads.jboss.org/overlord/dtgov/schemas/task-api-v1.xsd", "task");
    private final static QName _TaskSummary_QNAME = new QName("http://downloads.jboss.org/overlord/dtgov/schemas/task-api-v1.xsd", "taskSummary");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.overlord.dtgov.taskapi.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link FindTasksRequest }
     * 
     */
    public FindTasksRequest createFindTasksRequest() {
        return new FindTasksRequest();
    }

    /**
     * Create an instance of {@link TaskSummaryType }
     * 
     */
    public TaskSummaryType createTaskSummaryType() {
        return new TaskSummaryType();
    }

    /**
     * Create an instance of {@link TaskType }
     * 
     */
    public TaskType createTaskType() {
        return new TaskType();
    }

    /**
     * Create an instance of {@link FindTasksResponse }
     * 
     */
    public FindTasksResponse createFindTasksResponse() {
        return new FindTasksResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TaskType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://downloads.jboss.org/overlord/dtgov/schemas/task-api-v1.xsd", name = "task")
    public JAXBElement<TaskType> createTask(TaskType value) {
        return new JAXBElement<TaskType>(_Task_QNAME, TaskType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TaskSummaryType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://downloads.jboss.org/overlord/dtgov/schemas/task-api-v1.xsd", name = "taskSummary")
    public JAXBElement<TaskSummaryType> createTaskSummary(TaskSummaryType value) {
        return new JAXBElement<TaskSummaryType>(_TaskSummary_QNAME, TaskSummaryType.class, null, value);
    }

}
