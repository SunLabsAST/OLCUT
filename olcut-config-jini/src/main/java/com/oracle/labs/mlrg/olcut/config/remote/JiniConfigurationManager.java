package com.oracle.labs.mlrg.olcut.config.remote;

import com.oracle.labs.mlrg.olcut.config.ComponentListener;
import com.oracle.labs.mlrg.olcut.config.Configurable;
import com.oracle.labs.mlrg.olcut.config.ConfigurationManager;
import com.oracle.labs.mlrg.olcut.config.Options;
import com.oracle.labs.mlrg.olcut.config.PropertyException;
import com.oracle.labs.mlrg.olcut.config.PropertySheet;
import com.oracle.labs.mlrg.olcut.config.ArgumentException;
import com.oracle.labs.mlrg.olcut.config.InternalConfigurationException;
import com.oracle.labs.mlrg.olcut.config.RawPropertyData;
import com.oracle.labs.mlrg.olcut.config.UsageException;

import java.io.IOException;
import java.net.URL;
import java.rmi.Remote;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a set of <code>Configurable</code>s, their parametrization and the relationships between them. Configurations
 * can be specified either by xml or on-the-fly during runtime.
 *
 * This configuration manager has a Jini component registry and can look up or serve classes over the network.
 *
 * @see Configurable
 * @see PropertySheet
 */
public class JiniConfigurationManager extends ConfigurationManager {
    private static final Logger logger = Logger.getLogger(JiniConfigurationManager.class.getName());

    private ComponentRegistry registry;

    /**
     * Creates a new empty jini configuration manager. This constructor is only of use in cases when a system configuration
     * is created during runtime.
     */
    public JiniConfigurationManager() throws IOException {
        super();
    }

    /**
     * Creates a jini new configuration manager. Initial properties are loaded from the given location. No need to keep the notion
     * of 'context' around anymore we will just pass around this property manager.
     *
     * @param location place to load initial properties from
     * @throws IOException if an error occurs while loading properties from the URL
     */
    public JiniConfigurationManager(String location) throws IOException,
            PropertyException {
        super(location);

        //
        // Look up our distinguished registry name.
        setUpRegistry();
    }

    /**
     * Creates a new jini configuration manager. Initial properties are loaded from the given location. No need to keep the notion
     * of 'context' around anymore we will just pass around this property manager.
     *
     * @param url place to load initial properties from
     * @throws IOException if an error occurs while loading properties from the URL
     */
    public JiniConfigurationManager(URL url) throws IOException,
            PropertyException {
        super(url);

        //
        // Look up our distinguished registry name.
        setUpRegistry();
    }

    /**
     * Creates a new Jini configuration manager. Used when all the command line arguments are either: requests for the usage
     * statement, configuration file options, or unnamed.
     * @param arguments An array of command line arguments.
     * @throws UsageException Thrown when the user requested the usage string.
     * @throws ArgumentException Thrown when an argument fails to parse.
     * @throws PropertyException Thrown when an invalid property is loaded.
     * @throws IOException Thrown when the configuration file cannot be read.
     */
    public JiniConfigurationManager(String[] arguments) throws UsageException, ArgumentException, PropertyException, IOException {
        super(arguments,EMPTY_OPTIONS);

        //
        // Look up our distinguished registry name.
        setUpRegistry();
    }

    /**
     * Creates a new jini configuration manager.
     *
     * This constructor performs a sequence of operations:
     * - It validates the supplied options struct to make sure it does not have duplicate option names.
     * - Loads any configuration file specified by the {@link ConfigurationManager#configFileOption}.
     * - Parses any configuration overrides and applies them to the configuration manager.
     * - Parses out options for the supplied struct and writes them into the struct.
     * - Instantiates a jini component registry.
     * @param arguments An array of command line arguments.
     * @param options An object to write the parsed argument values into.
     * @throws UsageException Thrown when the user requested the usage string.
     * @throws ArgumentException Thrown when an argument fails to parse.
     * @throws PropertyException Thrown when an invalid property is loaded.
     * @throws IOException Thrown when the configuration file cannot be read.
     */
    public JiniConfigurationManager(String[] arguments, Options options) throws UsageException, ArgumentException, PropertyException, IOException {
        super(arguments,options);

        //
        // Look up our distinguished registry name.
        setUpRegistry();
    }

    /**
     * Adds a set of properties at the given URL to the current configuration
     * manager.
     */
    @Override
    public void addProperties(URL url) throws IOException, PropertyException {
        super.addProperties(url);

        if(registry == null) {
            setUpRegistry();
        }
    }

    /**
     * Makes sure that if a component registry is defined it is instantiated and
     * configured before anything else is looked up.
     */
    private void setUpRegistry() {
        PropertySheet ps = getPropertySheet("registry");
        if(ps == null) {
            return;
        }
        if(ComponentRegistry.class.isAssignableFrom(ps.getOwnerClass())) {
            registry = (ComponentRegistry) lookup("registry");
        }
    }
    
    /**
     * Gets a remote proxy suitable for passing to another object, if that is 
     * necessary.  Exporting the object is deemed necessary if the object to which 
     * we want to send the object is something that we looked up in the service
     * registrar.
     * 
     * If we haven't started a component registry, then the original object will
     * be returned.
     * 
     * @param r the object that we may want a proxy for
     * @param c the component to which we want to pass <code>r</code>
     * @return a proxy for the object, if one is required.
     */
    public Remote getRemote(Remote r, Configurable c) {
        if(registry == null) {
            return r;
        }
        return registry.getRemote(r, c);
    }
    
    /**
     * Gets a remote proxy suitable for passing over the wire.  If we haven't
     * started a component registry, then the original object will be returned.
     * Otherwise, we unconditionally return the remote handle for the object.
     * 
     * @param r the object that we want a proxy for
     * @return a proxy for the object, if one can be created
     */
    public Remote getRemote(Remote r) {
        if (registry == null) {
            return r;
        }
        return registry.getRemote(r);
    }
    
    /**
     * Indicates whether the given component was looked up in a service
     * registrar.
     * 
     * @param c the component to test.
     * @return <code>true</code> if the component was looked up in a service
     * registrar, <code>false</code> otherwise.
     */
    protected boolean wasLookedUp(Configurable c) {
        if(registry == null) {
            return false;
        }
        return registry.wasLookedUp(c);
    }
    
    /**
     * Shuts down the configuration manager, which just makes sure that any 
     * component registry that we may have is shut down.
     */
    @Override
    public synchronized void shutdown() {
        if(registry != null) {
            registry.shutdown();
            registry = null;
        }
    }

    /**
     * Returns the property sheet for the given object instance
     *
     * @param instanceName the instance name of the object
     * @return the property sheet for the object.
     */
    public ServablePropertySheet<? extends Configurable> getPropertySheet(String instanceName) {
        if(!symbolTable.containsKey(instanceName)) {
            // if it is not in the symbol table, so construct
            // it based upon our raw property data
            RawPropertyData rpd = rawPropertyMap.get(instanceName);
            if(rpd != null) {
                String className = rpd.getClassName();
                try {
                    Class cls = Class.forName(className);
                    if (Configurable.class.isAssignableFrom(cls)) {

                        // now load the property-sheet by using the class annotation
                        ServablePropertySheet<? extends Configurable> propertySheet =
                                getNewPropertySheet((Class<? extends Configurable>) cls,instanceName, this, rpd);

                        symbolTable.put(instanceName, propertySheet);
                    } else {
                        throw new PropertyException(instanceName, "Unable to cast " + className +
                                " to com.oracle.labs.mlrg.olcut.config.Configurable");
                    }
                } catch(ClassNotFoundException e) {
                    throw new PropertyException(e);
                }
            }
        }

        return (ServablePropertySheet<? extends Configurable>) symbolTable.get(instanceName);
    }

    /**
     * Looks up a configurable component by name. If a component registry exists
     * in the current configuration manager, it will be checked for the given
     * component name. 
     * 
     * @param instanceName the name of the component
     * @param cl a listener for this component that is notified when components
     * are added or removed
     * @param reuseComponent if <code>true</code>, then if the component was 
     * previously created that component will be returned.  If false, then a 
     * new component will be created regardless of whether it had been created
     * before.
     * @return the component, or null if a component was not found.
     * @throws InternalConfigurationException If the requested object could not be properly created, or is not a
     *                                        configurable object, or if an error occurred while setting a component
     *                                        property.
     */
    @Override
    public Configurable lookup(String instanceName, ComponentListener cl, boolean reuseComponent)
            throws InternalConfigurationException {
        // apply all new properties to the model
        instanceName = getStrippedComponentName(instanceName);
        Configurable ret = null;
        
        //
        // Get the property sheet for this component.
        ServablePropertySheet<? extends Configurable> ps = getPropertySheet(instanceName);
        
        if(ps == null) {
            return null;
        }
        
        //
        // If we have a registry and there's no properties in the property sheet
        // for this instance, then try to look it up in the registry.  This is
        // not perfect, because it will force lookups for components that just
        // happen to not have any properties, but it saves us from looking up
        // components that have local configurations and sitting through the 
        // component lookup timeout for the registry.  Life ain't perfect, eh?
        //
        // We'll also try a lookup if one is suggested by the importable attribute
        // for a component.
        if(registry != null && !ps.isExportable() &&
                ((ps.size() == 0 && ps.implementsRemote()) || ps.isImportable())) {
            logger.log(Level.FINER, "Attempted to lookup in registry");
            ret = registry.lookup(ps, cl);
        }

        //
        // Need we look farther?
        if(ret == null) {
            ret = super.lookup(instanceName, cl, reuseComponent);

            //
            // Do we need to export this?
            if(ps.isExportable() && registry != null) {
                registry.register(ret, ps);
            }
        }

        return ret;
    }

    /**
     * Looks up all components that have a given class name as their type.
     * @param c the class that we want to lookup
     * @param cl a listener that will report when components of the given type
     * are added or removed
     * @return a list of all the components with the given class name as their type.
     */
    public <T extends Configurable> List<T> lookupAll(Class<T> c, ComponentListener<T> cl) {

        List<T> ret = super.lookupAll(c,cl);
        if ((ret.size() == 0) && (registry != null)) {
            //
            // If we have a registry, then do a lookup for all things of the
            // given type.
            Configurable[] reg = registry.lookup(c, Integer.MAX_VALUE, cl);
            ret.addAll((List<T>)Arrays.asList(reg));
        }

        return ret;
    }

    /**
     * Looks up all components that have a given class name as their type.
     * @param c the class that we want to lookup
     * @param cl a listener that will report when components of the given type
     * are added or removed
     * @param entries Used to filter the looked up components.
     * @return a list of all the components with the given class name as their type.
     */
    public <T extends Configurable> List<T> lookupAll(Class<T> c, ComponentListener<T> cl, ConfigurationEntry[] entries) {

        List<T> ret = super.lookupAll(c,cl);
        if ((ret.size() == 0) && (registry != null)) {
            //
            // If we have a registry, then do a lookup for all things of the
            // given type.
            Configurable[] reg = registry.lookup(c, Integer.MAX_VALUE, cl, entries);
            ret.addAll((List<T>)Arrays.asList(reg));
        }

        return ret;
    }

    /**
     * Looks up all components that have a given class name as their type.
     * @param c the class that we want to lookup
     * @param cl a listener that will report when components of the given type
     * are added or removed
     * @param entries Used to filter the looked up components.
     * @return a list of all the components with the given class name as their type.
     */
    public <T extends Configurable> List<T> lookupAll(Class<T> c, ComponentListener<T> cl, String[] entries) {

        // Convert the entries from Strings into ConfigurationEntries
        ConfigurationEntry[] configEntries = null;
        if (entries != null) {
            configEntries = new ConfigurationEntry[entries.length];
            for (int i = 0; i < entries.length; i++) {
                configEntries[i] = new ConfigurationEntry(entries[i]);
            }
        }

        return lookupAll(c,cl,configEntries);
    }

    /**
     * Gets the component registry that is being used to register and lookup
     * components in a service registrar
     * @return the current component registry, or <code>null</code> if there 
     * isn't one.
     */
    public ComponentRegistry getComponentRegistry() {
        return registry;
    }

    /**
     * Test whether the given configuration manager instance equals this instance in terms of same configuration.
     * This equals implementation does not care about instantiation of components.
     */
    public boolean equals(Object obj) {
        if(!(obj instanceof JiniConfigurationManager)) {
            return false;
        }

        JiniConfigurationManager cm = (JiniConfigurationManager) obj;

        Collection<String> setA = new HashSet<String>(getComponentNames());
        Collection<String> setB = new HashSet<String>(cm.getComponentNames());
        if(!setA.equals(setB)) {
            return false;
        }

        // make sure that all components are the same
        for(String instanceName : getComponentNames()) {
            PropertySheet myPropSheet = getPropertySheet(instanceName);
            PropertySheet otherPropSheet = cm.getPropertySheet(instanceName);

            if(!otherPropSheet.equals(myPropSheet)) {
                return false;
            }
        }

        // make sure that both configuration managers have the same set of global properties
        return cm.getGlobalProperties().equals(getGlobalProperties());
    }

    /** Creates a deep copy of the given CM instance. */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("JiniConfigurationManager cannot be cloned.");
    }

    @Override
    protected <T extends Configurable> ServablePropertySheet<T> getNewPropertySheet(T conf, String name, ConfigurationManager cm, RawPropertyData rpd) {
        return new ServablePropertySheet<>(conf,name,(JiniConfigurationManager)cm,rpd);
    }

    @Override
    protected <T extends Configurable> ServablePropertySheet<T> getNewPropertySheet(Class<T> conf, String name, ConfigurationManager cm, RawPropertyData rpd) {
        return new ServablePropertySheet<>(conf,name,(JiniConfigurationManager)cm,rpd);
    }
}