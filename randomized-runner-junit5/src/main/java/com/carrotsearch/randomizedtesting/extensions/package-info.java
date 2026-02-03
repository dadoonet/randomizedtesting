/**
 * JUnit 5 Jupiter extensions for randomized testing.
 * 
 * <p>This package contains JUnit 5 extensions that replace the JUnit 4 rules 
 * from {@code com.carrotsearch.randomizedtesting.rules}.
 * 
 * <h2>Migration from JUnit 4 Rules</h2>
 * <table>
 *   <tr><th>JUnit 4 Rule</th><th>JUnit 5 Extension</th></tr>
 *   <tr><td>SystemPropertiesRestoreRule</td><td>{@link com.carrotsearch.randomizedtesting.extensions.SystemPropertiesRestoreExtension}</td></tr>
 *   <tr><td>SystemPropertiesInvariantRule</td><td>{@link com.carrotsearch.randomizedtesting.extensions.SystemPropertiesInvariantExtension}</td></tr>
 *   <tr><td>StaticFieldsInvariantRule</td><td>{@link com.carrotsearch.randomizedtesting.extensions.StaticFieldsInvariantExtension}</td></tr>
 *   <tr><td>RequireAssertionsRule</td><td>{@link com.carrotsearch.randomizedtesting.extensions.RequireAssertionsExtension}</td></tr>
 *   <tr><td>NoClassHooksShadowingRule</td><td>{@link com.carrotsearch.randomizedtesting.extensions.NoClassHooksShadowingExtension}</td></tr>
 *   <tr><td>NoInstanceHooksOverridesRule</td><td>{@link com.carrotsearch.randomizedtesting.extensions.NoInstanceHooksOverridesExtension}</td></tr>
 *   <tr><td>TestRuleAdapter</td><td>{@link com.carrotsearch.randomizedtesting.extensions.ExtensionAdapter}</td></tr>
 * </table>
 * 
 * @see com.carrotsearch.randomizedtesting.RandomizedExtension
 */
package com.carrotsearch.randomizedtesting.extensions;
