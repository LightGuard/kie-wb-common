/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.kie.workbench.common.screens.explorer.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Divider;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import org.guvnor.common.services.project.context.ProjectContext;
import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.AfterInitialization;
import org.kie.workbench.common.screens.explorer.client.resources.i18n.ProjectExplorerConstants;
import org.kie.workbench.common.screens.explorer.client.widgets.BranchChangeHandler;
import org.kie.workbench.common.screens.explorer.client.widgets.business.BusinessViewPresenterImpl;
import org.kie.workbench.common.screens.explorer.client.widgets.technical.TechnicalViewPresenterImpl;
import org.kie.workbench.common.screens.explorer.service.ExplorerService;
import org.kie.workbench.common.screens.explorer.service.Option;
import org.uberfire.client.annotations.DefaultPosition;
import org.uberfire.client.annotations.WorkbenchMenu;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.common.ContextDropdownButton;
import org.uberfire.client.mvp.UberView;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.model.CompassPosition;
import org.uberfire.workbench.model.Position;
import org.uberfire.workbench.model.menu.EnabledStateChangeListener;
import org.uberfire.workbench.model.menu.MenuCustom;
import org.uberfire.workbench.model.menu.MenuFactory;
import org.uberfire.workbench.model.menu.MenuItem;
import org.uberfire.workbench.model.menu.MenuPosition;
import org.uberfire.workbench.model.menu.Menus;

import static com.github.gwtbootstrap.client.ui.resources.ButtonSize.*;

/**
 * Repository, Package, Folder and File explorer
 */
@ApplicationScoped
@WorkbenchScreen(identifier = "org.kie.guvnor.explorer")
public class ExplorerPresenterImpl implements ExplorerPresenter {

    @Inject
    private ExplorerView view;

    @Inject
    private BusinessViewPresenterImpl businessViewPresenter;

    @Inject
    private TechnicalViewPresenterImpl technicalViewPresenter;

    @Inject
    private ProjectContext context;

    @Inject
    protected Caller<ExplorerService> explorerService;

    @Inject
    protected Event<ProjectContextChangeEvent> contextChangedEvent;

    private final NavLink businessView = new NavLink( ProjectExplorerConstants.INSTANCE.projectView() );
    private final NavLink techView = new NavLink( ProjectExplorerConstants.INSTANCE.repositoryView() );
    private final NavLink treeExplorer = new NavLink( ProjectExplorerConstants.INSTANCE.showAsFolders() );
    private final NavLink breadcrumbExplorer = new NavLink( ProjectExplorerConstants.INSTANCE.showAsLinks() );

    @Inject
    private ActiveContextOptions activeOptions;

    private String initPath = null;

    @AfterInitialization
    public void init() {
        addBranchChangeHandlers();
    }

    @OnStartup
    public void onStartup( final PlaceRequest placeRequest ) {
        final boolean noContextNavigationOption = ( Window.Location.getParameterMap().containsKey( "no_context_navigation" ) );
        final String paramExplorerMode = ( ( Window.Location.getParameterMap().containsKey( "explorer_mode" ) ) ? Window.Location.getParameterMap().get( "explorer_mode" ).get( 0 ) : "" ).trim();
        final String projectPathString = ( ( Window.Location.getParameterMap().containsKey( "path" ) ) ? Window.Location.getParameterMap().get( "path" ).get( 0 ) : null );

        this.initPath = placeRequest.getParameter( "init_path", projectPathString );
        final String explorerMode = placeRequest.getParameter( "mode", "" );
        final boolean noContext = placeRequest.getParameterNames().contains( "no_context" );

        if ( explorerMode.equalsIgnoreCase( "business_tree" ) ) {
            Collections.addAll( activeOptions.getOptions(),
                                Option.BUSINESS_CONTENT, Option.TREE_NAVIGATOR );
        } else if ( explorerMode.equalsIgnoreCase( "business_explorer" ) ) {
            Collections.addAll( activeOptions.getOptions(),
                                Option.BUSINESS_CONTENT, Option.BREADCRUMB_NAVIGATOR );
        } else if ( explorerMode.equalsIgnoreCase( "tech_tree" ) ) {
            Collections.addAll( activeOptions.getOptions(),
                                Option.TECHNICAL_CONTENT, Option.TREE_NAVIGATOR );
        } else if ( explorerMode.equalsIgnoreCase( "tech_explorer" ) ) {
            Collections.addAll( activeOptions.getOptions(),
                                Option.TECHNICAL_CONTENT, Option.BREADCRUMB_NAVIGATOR );
        } else if ( paramExplorerMode.equalsIgnoreCase( "business_tree" ) ) {
            Collections.addAll( activeOptions.getOptions(),
                                Option.BUSINESS_CONTENT, Option.TREE_NAVIGATOR );
        } else if ( paramExplorerMode.equalsIgnoreCase( "business_explorer" ) ) {
            Collections.addAll( activeOptions.getOptions(),
                                Option.BUSINESS_CONTENT, Option.BREADCRUMB_NAVIGATOR );
        } else if ( paramExplorerMode.equalsIgnoreCase( "tech_tree" ) ) {
            Collections.addAll( activeOptions.getOptions(),
                                Option.TECHNICAL_CONTENT, Option.TREE_NAVIGATOR );
        } else if ( paramExplorerMode.equalsIgnoreCase( "tech_explorer" ) ) {
            Collections.addAll( activeOptions.getOptions(),
                                Option.TECHNICAL_CONTENT, Option.BREADCRUMB_NAVIGATOR );
        }

        if ( noContext || noContextNavigationOption ) {
            activeOptions.getOptions().add( Option.NO_CONTEXT_NAVIGATION );
        }

        if ( activeOptions.getOptions().isEmpty() ) {
            explorerService.call( new RemoteCallback<Set<Option>>() {
                                      @Override
                                      public void callback( Set<Option> o ) {
                                          if ( o != null && !o.isEmpty() ) {
                                              activeOptions.getOptions().clear();
                                              activeOptions.getOptions().addAll( o );
                                          }
                                          config();
                                      }
                                  }, new ErrorCallback<Object>() {
                                      @Override
                                      public boolean error( Object o,
                                                            Throwable throwable ) {
                                          config();
                                          return false;
                                      }
                                  }).getLastUserOptions();

        } else {
            config();
        }
    }

    private void config() {
        businessView.setIconSize( IconSize.SMALL );
        businessView.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent clickEvent ) {
                if ( !activeOptions.getOptions().contains( Option.BUSINESS_CONTENT ) ) {
                    activateBusinessView();
                    selectBusinessView();
                    setupMenuItems();
                }
            }
        } );

        techView.setIconSize( IconSize.SMALL );
        techView.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent clickEvent ) {
                if ( !activeOptions.getOptions().contains( Option.TECHNICAL_CONTENT ) ) {
                    activateTechView();
                    selectTechnicalView();
                    setupMenuItems();
                }
            }
        } );

        treeExplorer.setIconSize( IconSize.SMALL );
        treeExplorer.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent clickEvent ) {
                if ( !activeOptions.getOptions().contains( Option.TREE_NAVIGATOR ) ) {
                    showTreeNav();
                    update();
                }
            }
        } );

        breadcrumbExplorer.setIconSize( IconSize.SMALL );
        breadcrumbExplorer.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent clickEvent ) {
                if ( !activeOptions.getOptions().contains( Option.BREADCRUMB_NAVIGATOR ) ) {
                    showBreadcrumbNav();
                    update();
                }
            }
        } );

        if ( activeOptions.getOptions().isEmpty() ) {
            activeOptions.getOptions().add( Option.BUSINESS_CONTENT);
            activeOptions.getOptions().add(Option.BREADCRUMB_NAVIGATOR);
            activeOptions.getOptions().add(Option.EXCLUDE_HIDDEN_ITEMS );
        }

        if ( activeOptions.getOptions().contains( Option.BUSINESS_CONTENT ) ) {
            activateBusinessView();
            selectBusinessView();

        } else if ( activeOptions.getOptions().contains( Option.TECHNICAL_CONTENT ) ) {
            activateTechView();
            selectTechnicalView();
        }

        setupMenuItems();
        update();
    }

    private void addBranchChangeHandlers() {
        BranchChangeHandler branchChangeHandler = new BranchChangeHandler() {

            @Override
            public void onBranchSelected( String branch ) {
                businessViewPresenter.branchChanged( branch );
                technicalViewPresenter.branchChanged( branch );

                ProjectContextChangeEvent event = new ProjectContextChangeEvent( context.getActiveOrganizationalUnit(),
                                                                                 context.getActiveRepository(),
                                                                                 context.getActiveProject(),
                                                                                 branch );

                contextChangedEvent.fire( event );
            }
        };

        businessViewPresenter.addBranchChangeHandler( branchChangeHandler );
        technicalViewPresenter.addBranchChangeHandler( branchChangeHandler );
    }

    private void setupMenuItems() {
        if ( activeOptions.getOptions() == null ) {
            return;
        }
        if ( activeOptions.getOptions().contains( Option.EXCLUDE_HIDDEN_ITEMS ) ) {
            excludeHiddenItems();
        } else {
            includeHiddenItems();
        }

        if ( activeOptions.getOptions().contains( Option.TREE_NAVIGATOR ) ) {
            showTreeNav();
        } else {
            showBreadcrumbNav();
        }
    }

    private void showBreadcrumbNav() {
        activeOptions.getOptions().add( Option.BREADCRUMB_NAVIGATOR );
        activeOptions.getOptions().remove( Option.TREE_NAVIGATOR );
        breadcrumbExplorer.setIcon( IconType.ASTERISK );
        treeExplorer.setIcon( null );
    }

    private void showTreeNav() {
        activeOptions.getOptions().remove( Option.BREADCRUMB_NAVIGATOR );
        activeOptions.getOptions().add( Option.TREE_NAVIGATOR );
        treeExplorer.setIcon( IconType.ASTERISK );
        breadcrumbExplorer.setIcon( null );
    }

    private void includeHiddenItems() {
        activeOptions.getOptions().add( Option.INCLUDE_HIDDEN_ITEMS );
        activeOptions.getOptions().remove( Option.EXCLUDE_HIDDEN_ITEMS );
    }

    private void excludeHiddenItems() {
        activeOptions.getOptions().remove( Option.INCLUDE_HIDDEN_ITEMS );
        activeOptions.getOptions().add( Option.EXCLUDE_HIDDEN_ITEMS );
    }

    @WorkbenchPartView
    public UberView<ExplorerPresenterImpl> getView() {
        return this.view;
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return ProjectExplorerConstants.INSTANCE.explorerTitle();
    }

    @DefaultPosition
    public Position getDefaultPosition() {
        return CompassPosition.WEST;
    }

    @WorkbenchMenu
    public Menus buildMenu() {
        return MenuFactory
                .newTopLevelCustomMenu( new MenuFactory.CustomMenuBuilder() {
                    @Override
                    public void push( MenuFactory.CustomMenuBuilder element ) {
                    }

                    @Override
                    public MenuItem build() {
                        return new MenuCustom<Widget>() {

                            @Override
                            public Widget build() {
                                return new ContextDropdownButton() {
                                    {
                                        displayCaret( false );
                                        setRightDropdown( true );
                                        setIcon( IconType.COG );
                                        setSize( MINI );

                                        add( businessView );
                                        add( techView );
                                        add( new Divider() );
                                        add( breadcrumbExplorer );
                                        add( treeExplorer );
//                                        add( new Divider() );
//                                        add( hiddenFiles );
                                    }
                                };
                            }

                            @Override
                            public boolean isEnabled() {
                                return false;
                            }

                            @Override
                            public void setEnabled( boolean enabled ) {

                            }

                            @Override
                            public String getContributionPoint() {
                                return null;
                            }

                            @Override
                            public String getCaption() {
                                return null;
                            }

                            @Override
                            public MenuPosition getPosition() {
                                return null;
                            }

                            @Override
                            public int getOrder() {
                                return 0;
                            }

                            @Override
                            public void addEnabledStateChangeListener( EnabledStateChangeListener listener ) {

                            }

                            @Override
                            public String getSignatureId() {
                                return null;
                            }

                            @Override
                            public Collection<String> getRoles() {
                                return null;
                            }

                            @Override
                            public Collection<String> getTraits() {
                                return null;
                            }
                        };
                    }
                } )
                .endMenu()
                .newTopLevelCustomMenu( new MenuFactory.CustomMenuBuilder() {
                    @Override
                    public void push( MenuFactory.CustomMenuBuilder element ) {
                    }

                    @Override
                    public MenuItem build() {
                        return new MenuCustom<Widget>() {

                            @Override
                            public Widget build() {
                                return new Button() {
                                    {
                                        setIcon( IconType.REFRESH );
                                        setSize( MINI );
                                        addClickHandler( new ClickHandler() {
                                            @Override
                                            public void onClick( ClickEvent event ) {
                                                refresh();
                                            }
                                        } );
                                    }
                                };
                            }

                            @Override
                            public boolean isEnabled() {
                                return false;
                            }

                            @Override
                            public void setEnabled( boolean enabled ) {

                            }

                            @Override
                            public String getContributionPoint() {
                                return null;
                            }

                            @Override
                            public String getCaption() {
                                return null;
                            }

                            @Override
                            public MenuPosition getPosition() {
                                return null;
                            }

                            @Override
                            public int getOrder() {
                                return 0;
                            }

                            @Override
                            public void addEnabledStateChangeListener( EnabledStateChangeListener listener ) {

                            }

                            @Override
                            public String getSignatureId() {
                                return null;
                            }

                            @Override
                            public Collection<String> getRoles() {
                                return null;
                            }

                            @Override
                            public Collection<String> getTraits() {
                                return null;
                            }
                        };
                    }
                } )
                .endMenu()
                .build();
    }

    private void activateBusinessView() {
        activeOptions.getOptions().add( Option.BUSINESS_CONTENT );
        activeOptions.getOptions().remove( Option.TECHNICAL_CONTENT );
        businessView.setIcon( IconType.CHECK );
        techView.setIcon( null );
    }

    @Override
    public void selectBusinessView() {
        businessViewPresenter.setVisible( true );
        technicalViewPresenter.setVisible( false );

        if ( initPath == null ) {
            businessViewPresenter.initialiseViewForActiveContext( context.getActiveOrganizationalUnit(),
                                                                  context.getActiveRepository(),
                                                                  context.getActiveProject(),
                                                                  context.getActivePackage() );

        } else {
            businessViewPresenter.initialiseViewForActiveContext( initPath );
            initPath = null;
        }
    }

    private void activateTechView() {
        activeOptions.getOptions().remove( Option.BUSINESS_CONTENT );
        activeOptions.getOptions().add( Option.TECHNICAL_CONTENT );
        techView.setIcon( IconType.CHECK );
        businessView.setIcon( null );
    }

    @Override
    public void selectTechnicalView() {
        businessViewPresenter.setVisible( false );
        technicalViewPresenter.setVisible( true );

        if ( initPath == null ) {
            technicalViewPresenter.initialiseViewForActiveContext( context.getActiveOrganizationalUnit(),
                                                                   context.getActiveRepository(),
                                                                   context.getActiveProject(),
                                                                   context.getActivePackage() );

        } else {
            technicalViewPresenter.initialiseViewForActiveContext( initPath );
            initPath = null;
        }
    }

    @Override
    public void refresh() {
        if ( businessViewPresenter.isVisible() ) {
            businessViewPresenter.refresh();
        } else if ( technicalViewPresenter.isVisible() ) {
            technicalViewPresenter.refresh();
        }
    }

    private void update() {
        if ( businessViewPresenter.isVisible() ) {
            businessViewPresenter.update( );

        } else if ( technicalViewPresenter.isVisible() ) {
            technicalViewPresenter.update( );
        }
    }
}
