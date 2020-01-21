import React, {Component} from 'react';
import {ThemeProvider, createTheme, Arwes, Footer, Button, Words, Content, Loading} from 'arwes';
import {INITIAL_VALUE, ReactSVGPanZoom, TOOL_AUTO} from 'react-svg-pan-zoom';
import {ReactSvgPanZoomLoader, SvgLoaderSelectElement} from 'react-svg-pan-zoom-loader'
import {
    BrowserRouter as Router,
    Switch,
    Route,
    Link
} from "react-router-dom";

class App extends Component {

    Viewer = null;

    constructor(props, context) {
        super(props, context);
        this.state = {
            landscapes: null,
            landscape: null,
            tool: TOOL_AUTO,
            value: INITIAL_VALUE
        }
    }

    componentDidMount() {
        this.getLandscapes();
    }

    changeValue(nextValue) {
        this.setState({value: nextValue})
    }

    fitToViewer() {
        this.Viewer.fitToViewer()
    }

    fitSelection() {
        this.Viewer.fitSelection(40, 40, 200, 200)
    }

    zoomOnViewerCenter() {
        this.Viewer.zoomOnViewerCenter(1.1)
    }

    getLandscapes() {
        fetch("http://localhost:8081/api/")
            .then((response) => {
                return response.json()
            })
            .then((json) => {
                this.setState({
                    landscapes: json
                })
            });
    }

    getMapData(landscape) {
        let params = new URLSearchParams(window.location.search);
        let data = params.get('data');
        if (data === undefined) {
            alert("data param missing");
            return;
        }

        fetch(data)
            .then((response) => {
                return response.json()
            })
            .then((json) => {
                this.setState({
                    mapData: json
                })
            });
    }

    onItemClick(l) {
        this.setState({landscape: l});
    }

    render() {
        return <Router>
            <ThemeProvider theme={createTheme()}>
                <Arwes>
                    <Switch>
                        <Route exact path="/" render={() => this.Home()}>
                        </Route>
                        <Route path="/landscape" render={() => this.Landscape()}>
                        </Route>
                    </Switch>
                    <Footer animate id={'footer'} style={{position: 'fixed', bottom: 0, width: '100%'}}>
                        <Link to="/"><Button animate>{'*'}</Button></Link>
                        <Words>{'Terminal here'}</Words>
                    </Footer>
                </Arwes>
            </ThemeProvider>
        </Router>


    }

    Home() {

        let landscapes = this.state.landscapes;
        let content;
        if (!landscapes) {
            content = <Loading animate/>;
        } else {
            content = landscapes.map(l => {
                return <Link
                    to="/landscape"><Button animate onClick={() => this.onItemClick(l)}>{l.name}</Button></Link>
            });
        }

        return (
            <Content style={{margin: 20}}>
                <h1>Landscapes</h1>
                {content}
            </Content>
        );
    }

    Landscape() {

        let landscapes = this.state.landscapes;
        let landscape = this.state.landscape;
        let content;
        if (!landscapes) {
            content = <Loading animate/>;
        } else {

            let data = 'http://localhost:8081/render/nivio:example/map.svg';
            /*let proxy = {
            <>
                <SvgLoaderSelectElement selector="#tree" onClick={this.onItemClick}
                                        stroke={'#111111'}/>
            </>
        };

             */
            return <ReactSvgPanZoomLoader src={data} render={(content) => (
                <div className="App">
                    <div style={{float: 'right'}}>
                        <button className="btn" onClick={() => this.zoomOnViewerCenter()}>Zoom in</button>
                        <button className="btn" onClick={() => this.fitSelection()}>Zoom area 200x200</button>
                        <button className="btn" onClick={() => this.fitToViewer()}>Fit</button>
                    </div>
                    <ReactSVGPanZoom key={'panzoom'}
                                     width={window.innerWidth * 0.95} height={window.innerHeight * 0.95}
                                     background={'transparent'}
                                     miniatureProps={{position: 'none'}} toolbarProps={{position: 'none'}}
                                     detectAutoPan={false}
                                     ref={Viewer => this.Viewer = Viewer}
                                     tool={this.state.tool} onChangeTool={tool => this.changeTool(tool)}
                                     value={this.state.value} onChangeValue={value => this.changeValue(value)}>
                        <svg>
                            {content}
                        </svg>
                    </ReactSVGPanZoom>
                </div>
            )}/>

        }


    }
}

export default App;
