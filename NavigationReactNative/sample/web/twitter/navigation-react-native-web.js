import React from 'react';
import {Image, Text, View, TouchableHighlight} from 'react-native';
import {NavigationContext} from 'navigation-react';

class NavigationBarWeb extends React.Component {
  constructor(props) {
    super(props);
    this.setTitle = this.setTitle.bind(this);
  }
  componentDidMount() {
    this.props.stateNavigator.onNavigate(this.setTitle);
    document.title = this.props.title;
  }
  componentWillUnmount() {
    this.props.stateNavigator.offNavigate(this.setTitle);    
  }
  setTitle(_oldState, _state, _data, _asyncData, stateContext) {
    const {crumbs} = this.props.stateNavigator.stateContext;
    if (stateContext.crumbs.length === crumbs.length)
      document.title = this.props.title;
  }
  render() {
    const {navigationImage, navigationHref, onNavigationPress, title, barTintColor, tintColor} = this.props;
    return (
      <View style={{
        paddingLeft: 15,
        paddingRight: 5,
        paddingBottom: 5,
        paddingTop: 5,
        flexDirection: 'row',
      }}>
       {navigationImage && <TouchableHighlight
          accessibilityRole="link"
          href={navigationHref}
          underlayColor={barTintColor}
          onPress={onNavigationPress}
          style={{marginRight: 20}}>
          <Image source={navigationImage} style={{width: 24, height: 24, tintColor}} />
        </TouchableHighlight>}
        <Text
          accessibilityRole="heading"
          aria-level="1"
          style={{fontSize: 20}}>
          {title}
        </Text>
      </View>
    );
  }
}

const NavigationBar = props => (
  <NavigationContext.Consumer>
      {({stateNavigator}) => (
        <NavigationBarWeb stateNavigator={stateNavigator} {...props} />
      )}
  </NavigationContext.Consumer>
)

const CoordinatorLayout = ({children}) => children;

export { NavigationBar, CoordinatorLayout };
