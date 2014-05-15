# Nodeset XML Format Parser Plugin

This is a "Resource Format Parser Plugin" for [Rundeck](http://rundeck.org).

It provides a new format for resource files, called **nodesetxml**.

This is based on the existing **resourcexml**, but enhances it with the idea of *Nodesets*.

## Nodesets

A `<nodeset>` defines a grouping of `<node>` elements.  Any attributes set for the Nodeset will be applied to all the enclosed nodes.

Nodesets can be nested.

Example:

```{.xml}
<nodeset environment="production">
    <nodeset application="tomcat" >
        <node name="tomcat01.mycompany.com"/>
        <node name="tomcat02.mycompany.com"/>
        <node name="tomcat03.mycompany.com"/>
        <node name="tomcat04.mycompany.com"/>
        <node name="tomcat05.mycompany.com"/>
        <node name="tomcat06.mycompany.com"/>
    </nodeset>
</nodeset>
```

This will produce these equivalent nodes if you were to define them in **resourcexml** format:

```{.xml}
<node name="tomcat01.mycompany.com" environment="production" application="tomcat"/>
<node name="tomcat02.mycompany.com" environment="production" application="tomcat"/>
<node name="tomcat03.mycompany.com" environment="production" application="tomcat"/>
<node name="tomcat04.mycompany.com" environment="production" application="tomcat"/>
<node name="tomcat05.mycompany.com" environment="production" application="tomcat"/>
<node name="tomcat06.mycompany.com" environment="production" application="tomcat"/>
```